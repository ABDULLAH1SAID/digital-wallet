ALTER TABLE transactions
    ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE transactions
    ADD CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key);
