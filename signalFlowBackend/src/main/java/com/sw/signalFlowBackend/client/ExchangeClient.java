package com.sw.signalFlowBackend.client;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ExchangeClient {
    Mono<BigDecimal> getPrice(String symbol);
    String getExchangeName();
}
