package com.sw.signalFlowBackend.service;

import com.sw.signalFlowBackend.entity.Order;
import com.sw.signalFlowBackend.enums.OrderSide;
import com.sw.signalFlowBackend.enums.OrderStatus;
import com.sw.signalFlowBackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeMonitorServiceImpl implements TradeMonitorService {

    private final OrderRepository orderRepository;
    private final PriceAggregatorService priceService;
    private final OrderService orderService;

    // Run every 3 seconds (3000 ms).
    // In Production: Use WebSockets. For Project: Polling is fine.
    @Override
    @Scheduled(fixedDelay = 3000)
    public void checkOpenPositions() {
        // 1. Fetch all ACTIVE trades
        List<Order> activeOrders = orderRepository.findByStatus(OrderStatus.FILLED);

        if (activeOrders.isEmpty()) return;

        // 2. Group by Symbol to optimize API calls
        // Map<"BTCUSDT", List<Order>>
        Map<String, List<Order>> ordersBySymbol = activeOrders.stream()
                .collect(Collectors.groupingBy(Order::getSymbol));

        // 3. Process each symbol
        ordersBySymbol.forEach((symbol, orders) -> {
            processSymbolGroup(symbol, orders);
        });
    }

    @Override
    public void processSymbolGroup(String symbol, List<Order> orders) {
        // Fetch price ONCE for all orders of this symbol
        priceService.getBestPrice(symbol)
                .subscribe(currentPrice -> {
                    for (Order order : orders) {
                        try {
                            evaluateRiskManagement(order, currentPrice);
                        } catch (Exception e) {
                            log.error("Error processing order {}: {}", order.getId(), e.getMessage());
                        }
                    }
                });
    }

    @Override
    public void evaluateRiskManagement(Order order, BigDecimal currentPrice) {
        BigDecimal entry = order.getEntryPrice();

        // LOGIC FOR BUY ORDERS (Long positions)
        if (order.getOrderSide() == OrderSide.BUY) {

            // A. CHECK STOP LOSS
            if (order.getStopLoss() != null && currentPrice.compareTo(order.getStopLoss()) <= 0) {
                orderService.closeOrder(order, currentPrice, "STOP_LOSS_HIT");
                return; // Stop further processing for this order
            }

            // B. CHECK TAKE PROFITS
            if (checkTakeProfit(order, currentPrice)) {
                return; // Trade closed
            }

            // C. BREAKEVEN LOGIC (Smart Feature)
            if (order.isMoveSlToBreakEven() && !order.isSlMovedToBreakEven()) {
                // If we passed TP1 (let's say we define "breakeven trigger" as 50% to TP1,
                // or just simply if current price > some threshold).
                // Simplified: If Price is > 1% above Entry, move SL to Entry.

                BigDecimal onePercentGain = entry.multiply(new BigDecimal("1.01"));

                if (currentPrice.compareTo(onePercentGain) > 0) {
                    log.info("Moving SL to Breakeven for Order {}", order.getId());
                    order.setStopLoss(entry); // Set SL to Entry Price
                    order.setSlMovedToBreakEven(true);
                    orderRepository.save(order);
                }
            }
        }

        // Logic for Sell ORDERS (Short positions) would be inverted (< SL, > TP)
    }

    @Override
    public boolean checkTakeProfit(Order order, BigDecimal currentPrice) {
        // Simple Logic: If ANY TP is hit, we close the WHOLE position.
        // Advanced Logic (Future): Close 30% at TP1, 30% at TP2...

        if (order.getTakeProfit1() != null && currentPrice.compareTo(order.getTakeProfit1()) >= 0) {
            orderService.closeOrder(order, currentPrice, "TAKE_PROFIT_1_HIT");
            return true;
        }
        if (order.getTakeProfit2() != null && currentPrice.compareTo(order.getTakeProfit2()) >= 0) {
            orderService.closeOrder(order, currentPrice, "TAKE_PROFIT_2_HIT");
            return true;
        }
        if (order.getTakeProfit3() != null && currentPrice.compareTo(order.getTakeProfit3()) >= 0) {
            orderService.closeOrder(order, currentPrice, "TAKE_PROFIT_3_HIT");
            return true;
        }
        return false;
    }

    @Override
    @Scheduled(fixedDelay = 3000)
    public void checkAllOrders() { // Renamed from checkOpenPositions
        // 1. Process ACTIVE (FILLED) trades (SL/TP)
        processActiveTrades();

        // 2. Process PENDING (LIMIT) trades
        processPendingOrders();
    }

    @Override
    public void processActiveTrades() {
        List<Order> activeOrders = orderRepository.findByStatus(OrderStatus.FILLED);
        if (activeOrders.isEmpty()) return;
        // ... (Insert logic from previous step: group by symbol -> processSymbolGroup)
        // (Ensure you call evaluateRiskManagement here)
    }

    @Override
    public void processPendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) return;

        Map<String, List<Order>> ordersBySymbol = pendingOrders.stream()
                .collect(Collectors.groupingBy(Order::getSymbol));

        ordersBySymbol.forEach((symbol, orders) -> {
            priceService.getBestPrice(symbol)
                    .subscribe(currentPrice -> {
                        for (Order order : orders) {
                            checkLimitMatch(order, currentPrice);
                        }
                    });
        });
    }

    @Override
    public void checkLimitMatch(Order order, BigDecimal currentPrice) {
        BigDecimal targetPrice = order.getInputPrice(); // The Limit Price set by user

        // BUY LIMIT: We want to buy CHEAP.
        // Logic: Execute if Current Price <= Target Price
        if (order.getOrderSide() == OrderSide.BUY) {
            if (currentPrice.compareTo(targetPrice) <= 0) {
                orderService.executePendingOrder(order, currentPrice);
            }
        }

        // SELL LIMIT: We want to sell HIGH.
        // Logic: Execute if Current Price >= Target Price
        else if (order.getOrderSide() == OrderSide.SELL) {
            if (currentPrice.compareTo(targetPrice) >= 0) {
                orderService.executePendingOrder(order, currentPrice);
            }
        }
    }
}