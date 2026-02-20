package com.sw.signalFlowBackend.controller;

import com.sw.signalFlowBackend.dto.WalletResponseDto;
import com.sw.signalFlowBackend.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    public Mono<WalletResponseDto> getWallet(@PathVariable Long userId){
        return walletService.getUserWallet(userId);
    }
}
