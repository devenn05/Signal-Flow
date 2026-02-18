package com.sw.signalFlowBackend.service.impl;

import com.sw.signalFlowBackend.client.exchange.BinanceClient;
import com.sw.signalFlowBackend.client.exchange.BybitClient;
import com.sw.signalFlowBackend.client.exchange.MexcClient;
import com.sw.signalFlowBackend.exception.SymbolNotFoundException;
import com.sw.signalFlowBackend.service.PriceAggregatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAggregatorServiceImpl implements PriceAggregatorService {

    private final BinanceClient binanceClient;
    private final BybitClient bybitClient;
    private final MexcClient mexcClient;

    @Override
    public Mono<BigDecimal> getBestPrice(String symbol){
        String cleanSymbol = symbol.toUpperCase();

        return binanceClient.getPrice(cleanSymbol)
                .doOnNext(price -> log.debug("Found {} on Binance: {}", cleanSymbol, price))
                .switchIfEmpty(Mono.defer(()->{
                    log.debug("{} not found on Binance, trying Bybit...", cleanSymbol);
                    return bybitClient.getPrice(cleanSymbol);
                })).switchIfEmpty(Mono.defer(()->{
                    log.debug("{} not found on Bybit, trying Mexc...", cleanSymbol);
                    return mexcClient.getPrice(cleanSymbol);
                })).switchIfEmpty(Mono.error(new SymbolNotFoundException(cleanSymbol)));

    }
}
