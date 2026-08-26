package com.example.digitalwallet.transaction.dto;

public record TransferResult(
        TransferResponse transfer,
        boolean created
) {
}
