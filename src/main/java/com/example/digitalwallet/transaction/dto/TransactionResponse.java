package com.example.digitalwallet.transaction.dto;

import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionStatus;
import com.example.digitalwallet.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long transactionId,
        Long walletId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String referenceId,
        TransactionStatus status,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getWallet().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getReferenceId(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
