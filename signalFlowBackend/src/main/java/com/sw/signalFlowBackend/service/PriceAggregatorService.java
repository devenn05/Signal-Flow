package com.sw.signalFlowBackend.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PriceAggregatorService {
    /**
     * Fetches the price of a symbol.
     * Strategy: Binance -> Bybit -> MEXC -> Error
     * @param symbol The trading pair (e.g., BTCUSDT)
     * @return Mono<BigDecimal> price
     */
    Mono<BigDecimal> getBestPrice(String symbol);
}
