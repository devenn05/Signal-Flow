package com.sw.signalFlowBackend.controller;

import com.sw.signalFlowBackend.dto.OrderRequestDto;
import com.sw.signalFlowBackend.entity.Order;
import com.sw.signalFlowBackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private OrderService orderService;

    @PostMapping("/{userId}")
    public Mono<ResponseEntity<Order>> placeOrder(@PathVariable Long userId, @Valid @RequestBody OrderRequestDto orderDto){
        return orderService.placeOrder(userId, orderDto)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }
}
