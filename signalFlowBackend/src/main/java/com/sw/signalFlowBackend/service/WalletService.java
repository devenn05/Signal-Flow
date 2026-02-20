package com.sw.signalFlowBackend.service;

import com.sw.signalFlowBackend.dto.WalletResponseDto;
import reactor.core.publisher.Mono;

public interface WalletService {
    Mono<WalletResponseDto> getUserWallet(Long userId);
}
