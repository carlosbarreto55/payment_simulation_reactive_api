-- Tabela de Payment Intents (Intenções de Pagamento)
CREATE TABLE payment_intents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE RESTRICT,
    INDEX idx_payment_intents_order_id (order_id),
    INDEX idx_payment_intents_status (status),
    INDEX idx_payment_intents_idempotency_key (idempotency_key),
    INDEX idx_payment_intents_payment_method (payment_method),
    INDEX idx_payment_intents_created_at (created_at),
    CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'DENIED', 'REFUNDED')),
    CHECK (payment_method IN ('PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'BOLETO')),
    CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Payment Transactions (Transações de Pagamento)
CREATE TABLE payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL,
    external_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    failure_reason TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id) ON DELETE CASCADE,
    INDEX idx_payment_transactions_payment_intent_id (payment_intent_id),
    INDEX idx_payment_transactions_external_id (external_id),
    INDEX idx_payment_transactions_status (status),
    INDEX idx_payment_transactions_processed_at (processed_at),
    CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'DENIED', 'REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

