package com.example.digitalwallet.wallet.dto;

import com.example.digitalwallet.wallet.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class WalletResponse {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private Instant createdAt;
    private Instant updatedAt;

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}