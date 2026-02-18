package com.sw.signalFlowBackend.controller;

import com.sw.signalFlowBackend.client.exchange.BinanceClient;
import com.sw.signalFlowBackend.client.exchange.BybitClient;
import com.sw.signalFlowBackend.client.exchange.MexcClient;
import com.sw.signalFlowBackend.service.PriceAggregatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final BinanceClient binanceClient;
    private final MexcClient mexcClient;
    private final BybitClient bybitClient;
    private final PriceAggregatorService priceAggregatorService;

    @GetMapping("binance/{symbol}")
    public Mono<BigDecimal> testBinance(@PathVariable String symbol){
        return binanceClient.getPrice(symbol);
    }

    @GetMapping("bybit/{symbol}")
    public Mono<BigDecimal> testBybit(@PathVariable String symbol){
        return bybitClient.getPrice(symbol);
    }

    @GetMapping("mexc/{symbol}")
    public Mono<BigDecimal> testMexc(@PathVariable String symbol){
        return mexcClient.getPrice(symbol);
    }

    @GetMapping("/smart-price/{symbol}")
    public Mono<BigDecimal> bestTest(@PathVariable String symbol) { return priceAggregatorService.getBestPrice(symbol);}
}
