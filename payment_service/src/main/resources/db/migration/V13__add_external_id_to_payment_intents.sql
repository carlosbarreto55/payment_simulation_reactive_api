ALTER TABLE payment_intents ADD COLUMN external_id VARCHAR(255) NULL;
CREATE INDEX idx_payment_intents_external_id ON payment_intents(external_id);
