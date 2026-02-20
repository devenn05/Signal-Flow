package com.sw.signalFlowBackend.service.impl;

import com.sw.signalFlowBackend.dto.AssetDto;
import com.sw.signalFlowBackend.dto.WalletResponseDto;
import com.sw.signalFlowBackend.entity.UserAsset;
import com.sw.signalFlowBackend.repository.UserAssetRepository;
import com.sw.signalFlowBackend.repository.UserRepository;
import com.sw.signalFlowBackend.service.PriceAggregatorService;
import com.sw.signalFlowBackend.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserAssetRepository userAssetRepository;
    private final UserRepository userRepository;
    private final PriceAggregatorService priceAggregatorService;

    @Override
    public Mono<WalletResponseDto> getUserWallet(Long userId){
        // 1. Fetch User (Blocking JPA -> Wrap in Mono)
        return Mono.fromCallable(() -> userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found")))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(user -> {

                    // 2. Fetch Assets
                    var assets = userAssetRepository.findByUserId(userId);

                    // 3. Process Assets Asynchronously (Fetch current prices)
                    return Flux.fromIterable(assets)
                            .flatMap(this::enrichAssetWithPrice)
                            .collectList()
                            .map(enrichedAssets -> {

                                // 4. Calculate Total Value
                                BigDecimal holdingsValue = enrichedAssets.stream()
                                        .map(a -> a.getAmount().multiply(a.getCurrentPrice()))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                return WalletResponseDto.builder()
                                        .usdtBalance(user.getBalance())
                                        .estimatedPortfolioValue(user.getBalance().add(holdingsValue))
                                        .assets(enrichedAssets)
                                        .build();
                            });
                });
    }

    private Mono<AssetDto> enrichAssetWithPrice(UserAsset asset) {
        if(asset.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            // Ignore empty assets or return basic info
            return Mono.just(AssetDto.builder()
                    .symbol(asset.getSymbol())
                    .amount(BigDecimal.ZERO)
                    .averageBuyPrice(asset.getAverageBuyPrice())
                    .currentPrice(BigDecimal.ZERO)
                    .pnl(BigDecimal.ZERO)
                    .build());
        }

        return priceAggregatorService.getBestPrice(asset.getSymbol())
                .onErrorReturn(BigDecimal.ZERO) // Fallback if API fails
                .map(currentPrice -> {

                    // PnL Calculation: ((Current - Avg) / Avg) * 100
                    BigDecimal pnl = BigDecimal.ZERO;
                    if(asset.getAverageBuyPrice().compareTo(BigDecimal.ZERO) > 0) {
                        pnl = currentPrice.subtract(asset.getAverageBuyPrice())
                                .divide(asset.getAverageBuyPrice(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }

                    return AssetDto.builder()
                            .symbol(asset.getSymbol())
                            .amount(asset.getAmount())
                            .averageBuyPrice(asset.getAverageBuyPrice())
                            .currentPrice(currentPrice)
                            .pnl(pnl)
                            .build();
                });
    }
}