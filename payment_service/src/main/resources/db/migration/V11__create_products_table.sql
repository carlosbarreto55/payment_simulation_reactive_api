CREATE TABLE products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    description     VARCHAR(1000),
    price           DECIMAL(19, 2)  NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'BRL',
    sku             VARCHAR(100)    NOT NULL,
    stock_quantity  INT             NOT NULL DEFAULT 0,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_products_sku UNIQUE (sku),
    INDEX idx_products_active (active),
    INDEX idx_products_sku (sku),
    CHECK (price > 0),
    CHECK (stock_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

