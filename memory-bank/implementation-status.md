# Implementation Status & Gaps

## Domain Completion Status

| Domain | Controllers | Services | Repositories | Entities | DTOs | Tests |
|--------|-------------|----------|--------------|----------|------|-------|
| **User/Auth** | 100% | 100% | 100% | 100% | 100% | Smoke only |
| **Customer** | 100% | 100% | 100% | 100% | 100% | — |
| **Product** | 100% | 100% | 100% | 100% | 100% | — |
| **Payment** | 50% | 50% | 100% | 100% | 80% | — |

---

## Implemented Features

### ✅ User & Authentication
- [x] User registration with BCrypt password hashing
- [x] Login with JWT access + refresh token generation
- [x] Token refresh with rotation (old token revoked)
- [x] Logout (refresh token revocation)
- [x] JWT validation on every request via WebFilter
- [x] RBAC with 4 roles and ownership validation

### ✅ Customer Management
- [x] Create customer (auto-linked to authenticated user)
- [x] Get customer by ID (with ownership/RBAC check)
- [x] Update customer (PATCH, partial)
- [x] Delete customer (hard delete)
- [x] Document validation and uniqueness (CPF/CNPJ/CNH)

### ✅ Product Management
- [x] Create product with SKU uniqueness
- [x] Get product by ID or SKU
- [x] List active products
- [x] List available products (in stock)
- [x] Update product
- [x] Deactivate product (soft-delete via active flag)
- [x] Stock decrease with availability validation
- [x] Stock increase

### ✅ Payment (Partial)
- [x] Create billing with PSP integration (AbacatePay)
- [x] Idempotency check (X-Idempotency-Key)
- [x] Payment intent persistence (PENDING → PROCESSING/DENIED)
- [x] Transaction logging
- [x] Amount calculation from product items

### ✅ Infrastructure
- [x] Health check (/actuator/health)
- [x] Prometheus metrics (/actuator/prometheus)
- [x] Centralized exception handling (GlobalExceptionHandler)
- [x] OpenAPI specification

---

## Missing / Gaps

### 🔴 Payment Domain
- [ ] Get payment intent by ID endpoint
- [ ] PSP webhook handler (HMAC validation, status updates)
- [ ] Payment confirmation flow
- [ ] Refund endpoint and logic
- [ ] Payment method validation/enrichment
- [ ] Webhook retry mechanism

### 🔴 Risk Analysis
- [ ] Service layer (schema exists in V5)
- [ ] Integration with payment flow (score-based approval/denial)

### 🔴 Outbox Pattern
- [ ] Event publishing service (schema exists in V7)
- [ ] Integration with payment flow (publish domain events)
- [ ] Background worker for processing outbox events

### 🔴 Audit Logging
- [ ] Service layer (schema exists in V8)
- [ ] Integration with critical operations (payment, refund, cancellation)

### 🔴 Testing
- [ ] Unit tests for services
- [ ] Integration tests with Testcontainers
- [ ] WebTestClient for controller tests
- [ ] Reactive stream testing (StepVerifier)

### 🟡 Technical Debt
- [ ] `PaymentMapper.toPaymentResponseDto()` uses deprecated `getAmount()` instead of calculating from product totals
- [ ] Dockerfile is placeholder (runs `top -b`, not the app)
- [ ] Port inconsistency: app=38080, docs=8080
- [ ] Public path inconsistency: `SecurityConfig` uses `/api/user**`, `JwtAuthWebFilter` uses `/api/auth/...`
- [ ] Order module dropped (V10), but some related code may remain
- [ ] No rate limiting configured
- [ ] No CORS configuration (may be needed for browser clients)
- [ ] Refresh token limit per user not enforced

---

## Schema-Ready But Not Used
| Migration | Table | Status |
|-----------|-------|--------|
| V5 | risk_analyses | Schema exists, no service |
| V6 | idempotency_keys | Schema exists, partially used in PaymentService |
| V7 | outbox_events | Schema exists, no service |
| V8 | audit_logs | Schema exists, no service |
