package com.example.digitalwallet.transaction.dto;

import com.example.digitalwallet.transaction.entity.Transaction;
import org.springframework.data.domain.Page;

import java.util.List;

public record TransactionPageResponse(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static TransactionPageResponse from(Page<Transaction> result) {
        return new TransactionPageResponse(
                result.getContent().stream().map(TransactionResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
