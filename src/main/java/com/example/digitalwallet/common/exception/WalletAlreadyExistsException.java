package com.example.digitalwallet.common.exception;

public class WalletAlreadyExistsException extends RuntimeException {
    public WalletAlreadyExistsException(Long userId) {
        super("Wallet already exists for user id: " + userId);
    }
}