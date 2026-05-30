ALTER TABLE customers ADD COLUMN IF NOT EXISTS document_type VARCHAR(10);
UPDATE customers SET document_type = 'CPF' WHERE document REGEXP '^[0-9]{11}$';
UPDATE customers SET document_type = 'CNPJ' WHERE document REGEXP '^[0-9]{14}$';
UPDATE customers SET document_type = 'CPF' WHERE document_type IS NULL;
ALTER TABLE customers MODIFY document_type VARCHAR(10) NOT NULL;
ALTER TABLE customers DROP INDEX IF EXISTS document;
ALTER TABLE customers ADD CONSTRAINT uk_customers_document_type UNIQUE (document, document_type);
