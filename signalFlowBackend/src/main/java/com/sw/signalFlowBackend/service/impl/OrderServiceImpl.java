package com.sw.signalFlowBackend.service.impl;

import com.sw.signalFlowBackend.dto.OrderRequestDto;
import com.sw.signalFlowBackend.entity.Order;
import com.sw.signalFlowBackend.entity.User;
import com.sw.signalFlowBackend.entity.UserAsset;
import com.sw.signalFlowBackend.enums.OrderSide;
import com.sw.signalFlowBackend.enums.OrderStatus;
import com.sw.signalFlowBackend.enums.OrderType;
import com.sw.signalFlowBackend.repository.OrderRepository;
import com.sw.signalFlowBackend.repository.UserAssetRepository;
import com.sw.signalFlowBackend.repository.UserRepository;
import com.sw.signalFlowBackend.service.OrderService;
import com.sw.signalFlowBackend.service.PriceAggregatorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private PriceAggregatorService priceAggregatorService;
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private UserAssetRepository userAssetRepository;

    @Override
    public Mono<Order> placeOrder(Long userId, OrderRequestDto orderDto){
        // 1. Validate Input (Basic)
        if (orderDto.getOrderType() == OrderType.LIMIT && orderDto.getPrice() ==null){
            return Mono.error(new IllegalArgumentException("Price is required for LIMIT Orders."));
        }

        // 2. Determine execution price
        Mono<BigDecimal> priceMono;
        if (orderDto.getOrderType() == OrderType.MARKET){
            priceMono = priceAggregatorService.getBestPrice(orderDto.getSymbol());
        } else {
            priceMono = Mono.just(orderDto.getPrice());
        }

        // 3. Process Order Pipeline
        return priceMono
                .publishOn(Schedulers.boundedElastic()) // Switch to blocking thread for JPA
                .map(executionPrice -> executeTradeLogic(userId, orderDto, executionPrice));
    }

    @Transactional
    protected Order executeTradeLogic(Long userId, OrderRequestDto dto, BigDecimal executionPrice) {
        // 1. Fetch User
        User user = userRepository.findById(userId).orElseThrow(()->new RuntimeException("User not found."));

        // 2. Calculate the cost
        BigDecimal quantity = dto.getQuantity();
        BigDecimal cost = executionPrice.multiply(dto.getPrice());

        // 3. Validate Funds & Update Balances (The Swap)
        // 2. Validate Funds & Update Balances (The Swap)
        if (dto.getOrderSide() == OrderSide.BUY) {
            handleBuySide(user, dto.getSymbol(), quantity, cost, executionPrice);
        } else {
            handleSellSide(user, dto.getSymbol(), quantity, cost);
        }

        Order order = Order.builder()
                .user(user)
                .symbol(dto.getSymbol().toUpperCase())
                .orderSide(dto.getOrderSide())
                .orderType(dto.getOrderType())
                .quantity(dto.getQuantity())
                .inputPrice(dto.getOrderType() == OrderType.LIMIT ? dto.getPrice() : null)
                .entryPrice(dto.getOrderType() == OrderType.MARKET ? executionPrice : null)
                .status(dto.getOrderType() == OrderType.MARKET ? OrderStatus.FILLED: OrderStatus.PENDING)
                .stopLoss(dto.getStopLoss())
                .takeProfit1(dto.getTakeProfit1())
                .takeProfit2(dto.getTakeProfit2())
                .takeProfit3(dto.getTakeProfit3())
                .moveSlToBreakEven(dto.isMoveToBreakEven())
                .isSlMovedToBreakEven(false)
                .build();

        return orderRepository.save(order);
    }

    private void handleBuySide(User user, String symbol, BigDecimal qty, BigDecimal totalCost, BigDecimal price){
        // A. Check USDT Balance
        if (user.getBalance().compareTo(totalCost) < 0){
            throw new RuntimeException("Insufficient funds. Required: " + totalCost + ", Available: " + user.getBalance());
        }

        // B. Deduct USDT
        user.setBalance(user.getBalance().subtract(totalCost));
        userRepository.save(user);

        // C. Update/Create Asset (User receives Coin)
        UserAsset asset = userAssetRepository.findByUserIdAndSymbol(user.getId(), symbol)
                .orElse(UserAsset.builder()
                        .user(user)
                        .symbol(symbol)
                        .amount(BigDecimal.ZERO)
                        .averageBuyPrice(BigDecimal.ZERO)
                        .build());


        // D. Calculate Weighted Average Price:
        // ((OldQty * OldPrice) + (NewQty * NewPrice)) / (OldQty + NewQty)
        BigDecimal currentTotalValue = asset.getAmount().multiply(asset.getAverageBuyPrice());
        BigDecimal newTotalValue = currentTotalValue.add(totalCost);
        BigDecimal newAmount = asset.getAmount().add(qty);

        // Avoid division by zero (shouldn't happen on add, but good practice)
        if (newAmount.compareTo(BigDecimal.ZERO) > 0) {
            asset.setAverageBuyPrice(newTotalValue.divide(newAmount, 8, RoundingMode.HALF_UP));
        }
        asset.setAmount(newAmount);
        userAssetRepository.save(asset);
    }

    private void handleSellSide(User user, String symbol, BigDecimal qty, BigDecimal totalProceeds) {
        // A. Check Asset Balance
        UserAsset asset = userAssetRepository.findByUserIdAndSymbol(user.getId(), symbol)
                .orElseThrow(() -> new RuntimeException("Insufficient asset balance. You do not own " + symbol));

        if (asset.getAmount().compareTo(qty) < 0) {
            throw new RuntimeException("Insufficient " + symbol + ". Owned: " + asset.getAmount() + ", Selling: " + qty);
        }

        // B. Deduct Asset
        asset.setAmount(asset.getAmount().subtract(qty));
        // Note: Average Buy Price does NOT change on Sell in standard accounting
        if (asset.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            // Optional: You could delete the row, but keeping it with 0 amount helps history
        }
        userAssetRepository.save(asset);

        // C. Add USDT to Balance
        user.setBalance(user.getBalance().add(totalProceeds));
        userRepository.save(user);
    }


    @Override
    @Transactional // IMPORTANT: Data consistency
    public void closeOrder(Order order, BigDecimal closingPrice, String reason) {
        if (order.getStatus() != OrderStatus.FILLED) {
            log.warn("Attempted to close order {} which is not FILLED", order.getId());
            return;
        }

        User user = order.getUser();
        BigDecimal quantity = order.getQuantity();
        BigDecimal exitValue = closingPrice.multiply(quantity);

        log.info("Closing Order ID: {} | Reason: {} | Exit Price: {} | PnL Value: {}",
                order.getId(), reason, closingPrice, exitValue);

        // 1. Logic Validation (Only Buy implemented for now as per previous step)
        if (order.getOrderSide() == OrderSide.BUY) {
            // A. Update Asset Balance (Remove the held coins)
            UserAsset asset = userAssetRepository.findByUserIdAndSymbol(user.getId(), order.getSymbol())
                    .orElseThrow(() -> new RuntimeException("Asset not found for closing order"));

            // Validate user actually has enough (should always be true in DB integrity)
            if(asset.getAmount().compareTo(quantity) < 0) {
                log.error("Critical: User asset data corrupt. Order qty > Asset qty");
                // In prod, you'd handle this specifically. For now, force close partial.
            }

            asset.setAmount(asset.getAmount().subtract(quantity));
            userAssetRepository.save(asset);

            // B. Credit USDT back to Balance
            user.setBalance(user.getBalance().add(exitValue));
            userRepository.save(user);
        }

        // 2. Update Order Status
        order.setStatus(OrderStatus.CLOSED);
        // We might want to store the "Exit Price" and "PnL" in the Order entity later
        // For now, we update the timestamp
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void executePendingOrder(Order order, BigDecimal fillPrice) {
        log.info("Executing PENDING Limit Order: {} at price {}", order.getId(), fillPrice);

        User user = order.getUser();
        BigDecimal cost = fillPrice.multiply(order.getQuantity());

        try {
            // 1. Re-validate Balance / Assets
            if (order.getOrderSide() == OrderSide.BUY) {
                // Calls the helper we wrote earlier (Asset Swap Logic)
                handleBuySide(user, order.getSymbol(), order.getQuantity(), cost, fillPrice);
            } else {
                handleSellSide(user, order.getSymbol(), order.getQuantity(), cost);
            }

            // 2. Update Order to FILLED
            order.setStatus(OrderStatus.FILLED);
            order.setEntryPrice(fillPrice); // Capture the actual fill price
            order.setUpdatedAt(LocalDateTime.now());

            // 3. Set Defaults for Auto-Monitor
            if (order.isMoveSlToBreakEven()) {
                order.setSlMovedToBreakEven(false);
            }

            orderRepository.save(order);
            log.info("Limit Order {} filled successfully.", order.getId());

        } catch (Exception e) {
            log.error("Failed to execute limit order {}: {}", order.getId(), e.getMessage());
            // If user spent their money elsewhere, CANCEL this order
            order.setStatus(OrderStatus.CLOSED);
            orderRepository.save(order);
        }
    }
}
