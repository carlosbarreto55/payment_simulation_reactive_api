-- Tabela de Outbox Events (Padrão Outbox para eventos assíncronos)
CREATE TABLE outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    INDEX idx_outbox_events_aggregate_type (aggregate_type),
    INDEX idx_outbox_events_aggregate_id (aggregate_id),
    INDEX idx_outbox_events_event_type (event_type),
    INDEX idx_outbox_events_status (status),
    INDEX idx_outbox_events_created_at (created_at),
    INDEX idx_outbox_events_status_created_at (status, created_at),
    CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

