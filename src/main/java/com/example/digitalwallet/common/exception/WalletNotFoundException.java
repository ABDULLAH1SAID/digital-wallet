package com.example.digitalwallet.common.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException() {
        super("Wallet not found for this user");
    }

    public WalletNotFoundException(Long walletId) {
        super("Wallet not found with id: " + walletId);
    }
}