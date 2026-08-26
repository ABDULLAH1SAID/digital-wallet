package com.example.digitalwallet.transaction.dto;

import com.example.digitalwallet.transaction.entity.TransactionStatus;

import java.math.BigDecimal;

public record TransferResponse(
        String referenceId,
        BigDecimal amount,
        TransactionStatus status,
        TransactionResponse debit,
        TransactionResponse credit
) {
}
