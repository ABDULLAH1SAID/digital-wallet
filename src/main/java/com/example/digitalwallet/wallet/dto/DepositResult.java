package com.example.digitalwallet.wallet.dto;

import com.example.digitalwallet.transaction.dto.TransactionResponse;

public record DepositResult(
        TransactionResponse transaction,
        boolean created
) {
}
