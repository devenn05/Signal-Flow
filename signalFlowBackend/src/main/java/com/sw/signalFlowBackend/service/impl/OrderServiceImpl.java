package com.sw.signalFlowBackend.service.impl;

import com.sw.signalFlowBackend.dto.OrderRequestDto;
import com.sw.signalFlowBackend.entity.Order;
import com.sw.signalFlowBackend.entity.User;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private PriceAggregatorService priceAggregatorService;
    private OrderRepository orderRepository;
    private UserRepository userRepository;

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
        BigDecimal cost = executionPrice.multiply(dto.getPrice());

        // C. Validate Balance (Logic depends on BUY or SELL side)
        // SIMPLE VERSION: We only check USDT balance for BUYs.
        // For SELLs, we would check Asset balance.

        if (dto.getOrderSide() == OrderSide.BUY){
            if (user.getBalance().compareTo(cost) < 0){
                throw new RuntimeException("Insufficient balance. Required: " + cost + ", Available: " + user.getBalance());
            }
            user.setBalance(user.getBalance().subtract(cost));
        }
        userRepository.save(user);

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
}
