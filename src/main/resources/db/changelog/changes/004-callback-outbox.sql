--liquibase formatted sql

--changeset external-bank:004-callback-outbox
CREATE TABLE callback_outbox (
    id         BIGSERIAL    PRIMARY KEY,
    payout_id  BIGINT       NOT NULL REFERENCES payout (id),
    url        VARCHAR(512) NOT NULL,
    send_at    TIMESTAMP    NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    sent_at    TIMESTAMP,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_callback_outbox_due ON callback_outbox (send_at) WHERE sent_at IS NULL;
--rollback DROP TABLE callback_outbox;
