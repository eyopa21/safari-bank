--liquibase formatted sql

--changeset external-bank:001-create-payout
CREATE TABLE payout (
    id           BIGSERIAL      PRIMARY KEY,
    payout_id    VARCHAR(64)    NOT NULL,
    bank_account VARCHAR(34)    NOT NULL,
    amount       NUMERIC(19, 4) NOT NULL,
    result       VARCHAR(16)    NOT NULL,
    bank_ref     VARCHAR(32),
    reason       VARCHAR(64),
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payout_payout_id UNIQUE (payout_id)
);
CREATE INDEX idx_payout_bank_account ON payout (bank_account);
--rollback DROP TABLE payout;
