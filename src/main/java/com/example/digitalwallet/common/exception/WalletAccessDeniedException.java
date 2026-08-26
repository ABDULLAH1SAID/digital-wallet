package com.example.digitalwallet.common.exception;

public class WalletAccessDeniedException extends RuntimeException {
    public WalletAccessDeniedException() {
        super("User does not own this wallet");
    }
}