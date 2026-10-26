--liquibase formatted sql

--changeset external-bank:002-add-payout-status
ALTER TABLE payout ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS';
ALTER TABLE payout ADD COLUMN settled_at TIMESTAMP;
UPDATE payout SET status = 'REJECTED' WHERE result = 'REJECTED';
--rollback ALTER TABLE payout DROP COLUMN settled_at;
--rollback ALTER TABLE payout DROP COLUMN status;
