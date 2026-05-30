# Database Schema

## Database: MySQL 8.0+

### Migration Strategy
Flyway with versioned migrations under `src/main/resources/db/migration/`

### Entity Relationship Summary

```
users ──1:N──> user_roles ──N:1──> roles
users ──1:1──> customers ──1:N──> documents
users ──1:N──> refresh_tokens
users ──1:N──> audit_logs

payment_intents ──1:N──> payment_transactions
payment_intents ──1:1──> risk_analyses

(outbox_events)  (independent event store)
(idempotency_keys) (global idempotency store)
```

---

## V1: Users and Roles

### `users`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | NOT NULL |
| name | VARCHAR(255) | NOT NULL |
| active | BOOLEAN | DEFAULT TRUE |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### `roles`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(50) | UNIQUE, NOT NULL |

### `user_roles`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK → users(id) |
| role_id | BIGINT | FK → roles(id) |
| UNIQUE(user_id, role_id) | | |

### `refresh_tokens`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK → users(id) |
| token | VARCHAR(500) | UNIQUE, NOT NULL |
| expires_at | TIMESTAMP | NOT NULL |
| revoked | BOOLEAN | DEFAULT FALSE |
| created_at | TIMESTAMP | NOT NULL |

---

## V2: Customers

### `customers`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| email | VARCHAR(255) | NOT NULL |
| phone | VARCHAR(20) | |
| user_id | BIGINT | FK → users(id), UNIQUE |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### `documents`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| customer_id | BIGINT | FK → customers(id) |
| type | VARCHAR(20) | NOT NULL (CPF, CNPJ, CNH) |
| value | VARCHAR(20) | NOT NULL, UNIQUE |

---

## V4: Payments

### `payment_intents`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| amount | DECIMAL(10,2) | NOT NULL |
| currency | VARCHAR(3) | DEFAULT 'BRL' |
| status | ENUM('PENDING','PROCESSING','APPROVED','DENIED','REFUNDED') | NOT NULL |
| payment_method | ENUM('PIX','CARD') | |
| frequency | ENUM('ONE_TIME','MULTIPLE_PAYMENTS') | |
| idempotency_key | VARCHAR(255) | |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### `payment_transactions`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| payment_intent_id | BIGINT | FK → payment_intents(id) |
| external_id | VARCHAR(255) | |
| status | VARCHAR(50) | |
| failure_reason | VARCHAR(255) | |
| processed_at | TIMESTAMP | |
| created_at | TIMESTAMP | NOT NULL |

---

## V5: Risk Analysis

### `risk_analyses`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| payment_intent_id | BIGINT | FK → payment_intents(id), UNIQUE |
| status | VARCHAR(50) | NOT NULL |
| score | INT | |
| reason | VARCHAR(255) | |
| analysed_at | TIMESTAMP | |
| created_at | TIMESTAMP | NOT NULL |

---

## V6: Idempotency

### `idempotency_keys`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| key_value | VARCHAR(255) | UNIQUE, NOT NULL |
| resource_type | VARCHAR(100) | NOT NULL |
| resource_id | VARCHAR(255) | |
| response_body | TEXT | |
| expires_at | TIMESTAMP | |
| created_at | TIMESTAMP | NOT NULL |

---

## V7: Outbox

### `outbox_events`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| aggregate_id | VARCHAR(255) | NOT NULL |
| aggregate_type | VARCHAR(255) | NOT NULL |
| event_type | VARCHAR(255) | NOT NULL |
| payload | TEXT | |
| status | ENUM('PENDING','PROCESSED','FAILED') | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| processed_at | TIMESTAMP | |

---

## V8: Audit Logs

### `audit_logs`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | FK → users(id) |
| action | VARCHAR(100) | NOT NULL |
| resource_type | VARCHAR(100) | NOT NULL |
| resource_id | VARCHAR(255) | |
| details | TEXT | |
| ip_address | VARCHAR(45) | |
| created_at | TIMESTAMP | NOT NULL |

---

## V11: Products

### `products`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| description | TEXT | |
| price | DECIMAL(10,2) | NOT NULL |
| currency | VARCHAR(3) | DEFAULT 'BRL' |
| sku | VARCHAR(100) | UNIQUE, NOT NULL |
| stock_quantity | INT | NOT NULL |
| active | BOOLEAN | DEFAULT TRUE |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |
