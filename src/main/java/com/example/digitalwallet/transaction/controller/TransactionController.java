package com.example.digitalwallet.transaction.controller;

import com.example.digitalwallet.common.security.CurrentUserId;
import com.example.digitalwallet.transaction.dto.TransactionPageResponse;
import com.example.digitalwallet.transaction.service.TransactionService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<TransactionPageResponse> getTransactions(
            @PathVariable @Positive Long walletId,
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(
                transactionService.getTransactions(userId, walletId, page, size, type, sort)
        );
    }
}
