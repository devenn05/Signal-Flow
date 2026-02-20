package com.sw.signalFlowBackend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WalletResponseDto {
    private BigDecimal usdtBalance;
    private BigDecimal estimatedPortfolioValue;
    private List<AssetDto> assets;
}
