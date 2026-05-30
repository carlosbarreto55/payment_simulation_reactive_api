# Domain Model

## User Domain

### Entities
| Entity | Fields | Notes |
|--------|--------|-------|
| **User** | id, email, password, name, active, createdAt, updatedAt | Core identity aggregate |
| **Role** | id, name | Pre-defined roles |
| **UserRole** | id, userId, roleId | Many-to-many join |
| **RefreshToken** | id, userId, token, expiresAt, revoked, createdAt | Token rotation support |

### Relationships
- User 1:N UserRole N:1 Role
- User 1:N RefreshToken

### Business Rules
- Email must be unique
- Password BCrypt-hashed
- Default role: CUSTOMER on registration
- Max 5 active refresh tokens per user (not enforced in code, schema allows)

---

## Customer Domain

### Entities
| Entity | Fields | Notes |
|--------|--------|-------|
| **Customer** | id, name, email, phone, userId, createdAt, updatedAt | Profile linked to user |
| **Document** | id, customerId, type, value | Embedded-like, separate table |

### Relationships
- User 1:1 Customer
- Customer 1:N Document

### Business Rules
- Customer linked to authenticated user on creation
- CUSTOMER role can only access own customer profile
- SUPPORT/SYSTEM roles can access any customer
- Documents must have valid type (CPF, CNPJ, CNH)

---

## Product Domain

### Entities
| Entity | Fields | Notes |
|--------|--------|-------|
| **Product** | id, name, description, price, currency, sku, stockQuantity, active, createdAt, updatedAt | Standalone product catalog |

### Business Rules
- SKU must be unique
- Price must be positive
- Stock quantity must be non-negative
- Deactivation is a soft-delete (active flag)
- `hasAvailableStock(quantity)` checks `stockQuantity >= quantity`
- `decreaseStock(quantity)` and `increaseStock(quantity)` mutate stock safely

---

## Payment Domain

### Entities
| Entity | Fields | Notes |
|--------|--------|-------|
| **PaymentIntent** | id, amount, currency, status, paymentMethod, frequency, idempotencyKey, createdAt, updatedAt | Main payment aggregate |
| **PaymentTransaction** | id, paymentIntentId, externalId, status, failureReason, processedAt, createdAt | Audit trail for PSP interactions |

### Enums
- **PaymentMethod**: PIX, CARD
- **Frequency**: ONE_TIME, MULTIPLE_PAYMENTS
- **PaymentStatus**: PENDING → PROCESSING → APPROVED | DENIED → REFUNDED

### Relationships
- PaymentIntent 1:N PaymentTransaction

### Business Rules
- Idempotency key must be unique per request
- PaymentIntent status transitions are forward-only
- Amount derived from product items (not directly input)

---

## Database Migrations Summary

| Migration | Entities Created |
|-----------|-----------------|
| V1 | users, roles, user_roles, refresh_tokens |
| V2 | customers, documents |
| V3 | orders, order_items (DROPPED in V10) |
| V4 | payment_intents, payment_transactions |
| V5 | risk_analyses |
| V6 | idempotency_keys |
| V7 | outbox_events |
| V8 | audit_logs |
| V9 | Seed default USER role |
| V10 | DROP orders, order_items; remove order FK |
| V11 | products |
