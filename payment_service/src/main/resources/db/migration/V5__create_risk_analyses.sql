-- Tabela de Risk Analyses (Análises de Risco/Antifraude)
CREATE TABLE risk_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL UNIQUE,
    score INT,
    decision VARCHAR(20) NOT NULL,
    reason TEXT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_risk_analyses_payment_intent_id (payment_intent_id),
    INDEX idx_risk_analyses_decision (decision),
    INDEX idx_risk_analyses_score (score),
    INDEX idx_risk_analyses_reviewed_by (reviewed_by),
    CHECK (decision IN ('APPROVE', 'REVIEW', 'DENY')),
    CHECK (score IS NULL OR (score >= 0 AND score <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

