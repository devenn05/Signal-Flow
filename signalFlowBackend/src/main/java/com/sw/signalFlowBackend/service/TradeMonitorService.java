package com.sw.signalFlowBackend.service;

import com.sw.signalFlowBackend.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public interface TradeMonitorService {
    void checkOpenPositions();
    void processSymbolGroup(String symbol, List<Order> orders);
    void evaluateRiskManagement(Order order, BigDecimal currentPrice);
    boolean checkTakeProfit(Order order, BigDecimal currentPrice);
    void checkAllOrders();
    void processActiveTrades();
    void processPendingOrders();
    void checkLimitMatch(Order order, BigDecimal currentPrice);

}
