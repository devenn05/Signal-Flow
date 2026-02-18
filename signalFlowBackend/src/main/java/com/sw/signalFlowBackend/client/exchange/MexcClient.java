package com.sw.signalFlowBackend.client.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.sw.signalFlowBackend.client.ExchangeClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class MexcClient implements ExchangeClient {
    private final WebClient webClient;

    public MexcClient(@Qualifier("mexcWebClient") WebClient webClient){
        this.webClient = webClient;
    }

    @Override
    public Mono<BigDecimal> getPrice(String symbol){
        try {
            return webClient.get()
                    .uri("/api/v3/ticker/price?symbol={symbol}", symbol)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(json -> {
                        JsonNode priceNode = json.path("price");
                        if (priceNode.isMissingNode()) {
                            throw new RuntimeException("MEXC price data missing for: " + symbol);
                        }
                        return new BigDecimal(priceNode.asText());
                    });
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    @Override
    public String getExchangeName(){
        return "MEXC";
    }
}
