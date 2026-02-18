package com.sw.signalFlowBackend.dto;

import com.sw.signalFlowBackend.enums.OrderSide;
import com.sw.signalFlowBackend.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequestDto {

    @NotNull(message = "System is required")
    private String symbol;

    @NotNull(message = "Trade Direction is required.")
    private OrderSide orderSide;

    @NotNull(message = "Trade type is required")
    private OrderType orderType;

    @NotNull
    @DecimalMin(value = "0.00000001", message = "Quantity must be positive")
    private BigDecimal quantity;

    // Optional: Only required if type == LIMIT
    private BigDecimal price;

    // Optional Risk Management
    private BigDecimal stopLoss;
    private BigDecimal takeProfit1;
    private BigDecimal takeProfit2;
    private BigDecimal takeProfit3;

    // Feature request: BreakEven
    private boolean moveToBreakEven;
}
