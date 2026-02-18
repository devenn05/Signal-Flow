package com.sw.signalFlowBackend.client.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.sw.signalFlowBackend.client.ExchangeClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class BybitClient implements ExchangeClient {
    private final WebClient webClient;

    public BybitClient(@Qualifier("bybitWebClient") WebClient webClient){
        this.webClient = webClient;
    }

    @Override
    public Mono<BigDecimal> getPrice(String symbol){
        try {
            return webClient.get()
                    .uri("/v5/market/tickers?category=spot&symbol={symbol}", symbol)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(json -> {
                        JsonNode listNode = json.path("result").path("list");
                        if (listNode.isMissingNode() || !listNode.isArray() || listNode.isEmpty()) {
                            throw new RuntimeException("Bybit data missing for symbol: " + symbol);
                        }
                        String lastPrice = listNode.get(0).path("lastPrice").asText();
                        return new BigDecimal(lastPrice);
                    });
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    @Override
    public String getExchangeName(){
        return "BYBIT";
    }
}
