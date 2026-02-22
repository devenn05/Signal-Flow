package com.sw.signalFlowBackend.controller;

import com.sw.signalFlowBackend.dto.OrderRequestDto;
import com.sw.signalFlowBackend.entity.Order;
import com.sw.signalFlowBackend.enums.OrderStatus;
import com.sw.signalFlowBackend.repository.OrderRepository;
import com.sw.signalFlowBackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private OrderService orderService;
    private final OrderRepository orderRepository;

    @PostMapping("/{userId}")
    public Mono<ResponseEntity<Order>> placeOrder(@PathVariable Long userId, @Valid @RequestBody OrderRequestDto orderDto){
        return orderService.placeOrder(userId, orderDto)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<Order>> getOrderHistory(@PathVariable Long userId) {
        // In a real industrial app, you would use Pagination (Pageable) here
        return ResponseEntity.ok(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    // Get Active Orders only
    @GetMapping("/{userId}/active")
    public ResponseEntity<List<Order>> getActiveOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderRepository.findByUserIdAndStatus(userId, OrderStatus.FILLED));
    }

    // Cancel an Order manually
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        // logic to fetch order, check if PENDING, set to CANCELLED
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if(order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CLOSED);
            orderRepository.save(order);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
