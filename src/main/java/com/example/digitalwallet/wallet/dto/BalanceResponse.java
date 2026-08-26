package com.example.digitalwallet.wallet.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        Long walletId,
        BigDecimal balance
) {
}
