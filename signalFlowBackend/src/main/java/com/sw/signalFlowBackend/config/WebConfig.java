package com.sw.signalFlowBackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfig {

    // Centralized URLs
    public static final String BINANCE_URL = "https://api.binance.com";
    public static final String MEXC_URL = "https://api.mexc.com";
    public static final String BYBIT_URL = "https://api.bybit.com";

    @Bean
    public WebClient binanceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(BINANCE_URL).build();
    }

    @Bean
    public WebClient mexcWebClient(WebClient.Builder builder) {
        return builder.baseUrl(MEXC_URL).build();
    }

    @Bean
    public WebClient bybitWebClient(WebClient.Builder builder) {
        return builder.baseUrl(BYBIT_URL).build();
    }
}
