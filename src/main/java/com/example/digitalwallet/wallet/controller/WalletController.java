package com.example.digitalwallet.wallet.controller;
import com.example.digitalwallet.wallet.dto.DepositResult;
import com.example.digitalwallet.common.security.CurrentUserId;
import com.example.digitalwallet.transaction.dto.TransactionResponse;
import com.example.digitalwallet.wallet.dto.BalanceResponse;
import com.example.digitalwallet.wallet.dto.DepositRequest;
import com.example.digitalwallet.wallet.dto.DepositResult;
import com.example.digitalwallet.wallet.dto.WalletResponse;
import com.example.digitalwallet.wallet.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Validated
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@CurrentUserId Long userId) {
        WalletResponse response = walletService.createWallet(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable @Positive Long walletId,
            @CurrentUserId Long userId
    ) {
        return ResponseEntity.ok(walletService.getBalance(userId, walletId));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @CurrentUserId Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request
    ) {
        DepositResult result = walletService.deposit(userId, idempotencyKey, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.transaction());
    }

}