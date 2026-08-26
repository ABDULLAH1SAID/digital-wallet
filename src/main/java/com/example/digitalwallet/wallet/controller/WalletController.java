package com.example.digitalwallet.wallet.controller;

import com.example.digitalwallet.common.security.CurrentUserId;
import com.example.digitalwallet.wallet.dto.WalletResponse;
import com.example.digitalwallet.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@CurrentUserId Long userId) {
        WalletResponse response = walletService.createWallet(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}