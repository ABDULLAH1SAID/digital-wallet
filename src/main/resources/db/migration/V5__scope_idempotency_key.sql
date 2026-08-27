ALTER TABLE transactions
    ADD COLUMN operation VARCHAR(20);

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_operation
        CHECK (operation IS NULL OR operation IN ('DEPOSIT', 'TRANSFER'));

UPDATE transactions
SET operation = 'DEPOSIT'
WHERE idempotency_key IS NOT NULL
  AND type = 'CREDIT';

UPDATE transactions
SET operation = 'TRANSFER'
WHERE idempotency_key IS NOT NULL
  AND type = 'DEBIT';

ALTER TABLE transactions
DROP CONSTRAINT uk_transactions_idempotency_key;

ALTER TABLE transactions
    ADD CONSTRAINT uk_transactions_wallet_operation_idempotency
        UNIQUE (wallet_id, operation, idempotency_key);