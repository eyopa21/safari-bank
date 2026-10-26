--liquibase formatted sql

--changeset external-bank:003-add-payout-callback
ALTER TABLE payout ADD COLUMN event_id VARCHAR(64);
ALTER TABLE payout ADD COLUMN outcome VARCHAR(16);
ALTER TABLE payout ADD COLUMN callback_sent_at TIMESTAMP;
ALTER TABLE payout ADD CONSTRAINT uk_payout_event_id UNIQUE (event_id);
UPDATE payout SET outcome = status WHERE status IN ('SUCCEEDED', 'FAILED');
--rollback ALTER TABLE payout DROP COLUMN callback_sent_at;
--rollback ALTER TABLE payout DROP COLUMN outcome;
--rollback ALTER TABLE payout DROP COLUMN event_id;
