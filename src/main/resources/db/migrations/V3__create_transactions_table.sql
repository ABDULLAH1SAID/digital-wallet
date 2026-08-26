CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              wallet_id BIGINT NOT NULL,
                              type VARCHAR(10) NOT NULL,
                              amount NUMERIC(19, 2) NOT NULL,
                              balance_after NUMERIC(19, 2) NOT NULL,
                              counterparty_wallet_id BIGINT,
                              reference_id VARCHAR(50) NOT NULL,
                              status VARCHAR(20) NOT NULL,
                              description VARCHAR(255),
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                              CONSTRAINT fk_transactions_wallet
                                  FOREIGN KEY (wallet_id)
                                      REFERENCES wallets(id),

                              CONSTRAINT fk_transactions_counterparty_wallet
                                  FOREIGN KEY (counterparty_wallet_id)
                                      REFERENCES wallets(id),

                              CONSTRAINT chk_transactions_type
                                  CHECK (type IN ('CREDIT', 'DEBIT')),

                              CONSTRAINT chk_transactions_status
                                  CHECK (status IN ('COMPLETED', 'PENDING', 'FAILED'))
);

CREATE INDEX idx_transactions_wallet_created
    ON transactions (wallet_id, created_at DESC);