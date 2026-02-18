package com.sw.signalFlowBackend.service;

import com.sw.signalFlowBackend.dto.OrderRequestDto;
import com.sw.signalFlowBackend.entity.Order;

public interface OrderService {
    Mono<Order> placeOrder(Long userId, OrderRequestDto orderDto);
}
