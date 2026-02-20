package com.sw.signalFlowBackend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AssetDto {
    private String symbol;
    private BigDecimal amount;
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal pnl;
}
