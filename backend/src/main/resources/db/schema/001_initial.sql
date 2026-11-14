-- Reviewed initial PostgreSQL schema. Run manually against the intended branch; back it up first.
-- This script creates only the Ranger tables, uses no Flyway, and does not import data.
BEGIN;
CREATE TABLE IF NOT EXISTS ranger_receipts (
  id UUID PRIMARY KEY, owner_uid VARCHAR(128) NOT NULL, source_key VARCHAR(180) NOT NULL,
  merchant VARCHAR(120) NOT NULL, category VARCHAR(32) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
  approximate_time BOOLEAN NOT NULL, total NUMERIC(12,2) NOT NULL, fee_total NUMERIC(12,2) NOT NULL,
  source VARCHAR(32) NOT NULL, review_required BOOLEAN NOT NULL,
  CONSTRAINT uk_receipt_owner_source UNIQUE(owner_uid,source_key)
);
CREATE INDEX IF NOT EXISTS idx_receipt_owner_time ON ranger_receipts(owner_uid,occurred_at);
CREATE TABLE IF NOT EXISTS ranger_items (
  id UUID PRIMARY KEY, receipt_id UUID NOT NULL REFERENCES ranger_receipts(id) ON DELETE CASCADE,
  name VARCHAR(160) NOT NULL, amount NUMERIC(12,2), junk_food BOOLEAN NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ranger_items_receipt ON ranger_items(receipt_id);
CREATE TABLE IF NOT EXISTS ranger_fees (
  id UUID PRIMARY KEY, receipt_id UUID NOT NULL REFERENCES ranger_receipts(id) ON DELETE CASCADE,
  label VARCHAR(120) NOT NULL, amount NUMERIC(12,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ranger_fees_receipt ON ranger_fees(receipt_id);
COMMIT;
