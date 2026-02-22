package com.sw.signalFlowBackend.repository;

import com.sw.signalFlowBackend.entity.Order;
import com.sw.signalFlowBackend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
}
