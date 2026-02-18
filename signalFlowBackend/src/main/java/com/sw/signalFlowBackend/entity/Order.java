package com.sw.signalFlowBackend.entity;

import com.sw.signalFlowBackend.enums.OrderSide;
import com.sw.signalFlowBackend.enums.OrderStatus;
import com.sw.signalFlowBackend.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders", indexes = {@Index(name = "idx_user_status", columnList = "user_id, status")})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide orderSide;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(precision = 19, scale = 8)
    private BigDecimal inputPrice;

    @Column(precision = 19, scale = 8)
    private BigDecimal entryPrice;

    // --- Risk Management ---

    @Column(precision = 19, scale = 8)
    private BigDecimal stopLoss;

    @Column(precision = 19, scale = 8)
    private BigDecimal takeProfit1;

    @Column(precision = 19, scale = 8)
    private BigDecimal takeProfit2;

    @Column(precision = 19, scale = 8)
    private BigDecimal takeProfit3;

    // --- Breakeven Logic ---
    // If true, the system should move SL to EntryPrice when TP1 is hit
    @Column(name = "move_sl_to_be")
    private boolean moveSlToBreakEven;

    // Internal flag to check if SL has already been moved to prevent redundant updates
    @Column(name = "is_sl_moved")
    private boolean isSlMovedToBreakEven;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
