ALTER TABLE wallets
    ADD CONSTRAINT chk_wallets_balance_non_negative
        CHECK (balance >= 0);

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_amount_positive
        CHECK (amount > 0);