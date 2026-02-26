-- Remove FK from payment_intents to orders before dropping orders tables
ALTER TABLE payment_intents DROP FOREIGN KEY payment_intents_ibfk_1;
ALTER TABLE payment_intents DROP INDEX idx_payment_intents_order_id;
ALTER TABLE payment_intents DROP COLUMN order_id;

-- Update payment_method constraint to support new method values (PIX, CARD)
ALTER TABLE payment_intents DROP CHECK payment_intents_chk_2;
ALTER TABLE payment_intents MODIFY payment_method VARCHAR(50) NOT NULL;

-- Remove orders and order_items tables (no longer used in this service)
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;

