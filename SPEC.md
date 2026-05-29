# SPEC.md — Project Specification & Architecture

> **Living document.** Updated with every code change to reflect the current state of the codebase vs. the target state.

---

## 1. Project Identity

| Field | Value |
|---|---|
| **Name** | Payment Platform (formerly "Magalu Checkout & Payment Platform") |
| **Purpose** | Reactive payment platform integrating with AbacatePay as study of DDD + Spring WebFlux |
| **Base package** | `checkout` |
| **Build tool** | Maven (`mvnw`), Spring Boot 3.5.3, Java 17 |
| **Repository** | Monolith under `payment_service/` |

---

## 2. Current Architecture (as of 2026-05-28)

### 2.1 Structural Overview

```
checkout/
├── PaymentServiceApplication.java       # Entry point
├── common/                               # Shared cross-cutting
│   ├── ApiConstants.java
│   ├── HeaderProcessor.java
│   ├── enums/DocumentType.java
│   └── exception/                        # 18 exception classes + handler
├── config/
│   ├── infrastructure/
│   │   ├── PaymentProviderConfig.java    # WebClient bean for AbacatePay
│   │   └── R2dbcConfig.java             # R2DBC repository scanning
│   └── security/
│       ├── JwtAuthToken.java            # Custom Authentication impl
│       ├── JwtAuthWebFilter.java        # JWT extraction + validation filter
│       └── SecurityConfig.java          # WebFlux security rules
└── domain/
    ├── user/         # Auth: register, login, refresh-token, JWT
    ├── customer/     # Customer CRUD with document validation
    ├── payment/      # Billing creation, AbacatePay integration
    ├── product/      # Product CRUD with stock management
    └── paymentProvider/  # WebClient to AbacatePay (POST /v1/billing/create)
```

### 2.2 Architecture Style

**Currently: Layered Architecture (NOT DDD).**

- **API Layer**: Spring `@RestController` classes with `@Valid` DTOs
- **Service Layer**: Procedural `@Service` classes orchestrating repositories + WebClient
- **Data Layer**: Spring Data R2DBC `@Repository` interfaces
- **Entities**: Anemic `@Data @Builder` classes (data bags with no behavior, except `Product` which has `decreaseStock`/`increaseStock`)

No ports, no adapters, no domain events, no aggregate roots, no value objects (except `Document` which is embedded in `Customer`), no bounded contexts. The package names say "domain" but the code is layered.

### 2.3 Current Domain Model

#### User Context (`domain/user`)
| Class | Type | Purpose |
|---|---|---|
| `User` | Anemic entity | `users` table row (id, email, passwordHash, enabled, timestamps) |
| `Role` | Anemic entity | `roles` table row (id, name) |
| `UserRole` | Anemic entity | Join table `user_roles` |
| `RefreshToken` | Anemic entity | `refresh_tokens` table row |

**Services**: `UserService` (register, load-by-email, promote-to-admin), `AuthenticationService` (login, refresh-token, logout, token generation/storage), `JwtService` (token generation, validation)

#### Customer Context (`domain/customer`)
| Class | Type | Purpose |
|---|---|---|
| `Customer` | Anemic entity | `customers` table (id, userId FK, name, email, phoneNumber, document) |
| `Document` | Embedded data class | `documentType` + `documentNumber` |

**Service**: `CustomerService` — CRUD with role-based authorization (user can only access own customer; SUPPORT can access all)

#### Payment Context (`domain/payment`)
| Class | Type | Purpose |
|---|---|---|
| `PaymentIntent` | Anemic entity | `payment_intents` table (amount, currency, status as raw String, paymentMethod, idempotencyKey) |
| `PaymentTransaction` | Anemic entity | `payment_transactions` table (externalId, status, failureReason) |
| `PaymentMethod` | Enum | `PIX`, `CARD` |
| `Frequency` | Enum | `ONE_TIME`, `MULTIPLE_PAYMENTS` |

**Service**: `PaymentService` — `createBilling()` is a long procedural method that checks idempotency, saves a PaymentIntent, calls AbacatePay, saves a PaymentTransaction, updates intent status.

**Empty files**: `PaymentTransactionService.java`, `PaymentWebhookHandler.java`

#### Product Context (`domain/product`)
| Class | Type | Purpose |
|---|---|---|
| `Product` | Entity with SOME behavior | `products` table with `decreaseStock()`, `increaseStock()`, `hasAvailableStock()` — the only entity with domain logic |

**Service**: `ProductService` — Full CRUD + stock operations

### 2.4 Anti-Corruption Layer (Non-Existent)

The `PaymentServiceClient` (a WebClient wrapper) and the external DTOs (`CreateBillingRequestExtDto`, `CreateBillingResponseExtDto`) live **inside** `domain/paymentProvider/` and `domain/payment/dto/`. This means:
- Provider-specific JSON shapes leak into domain DTOs
- No translation layer between external contract and domain model
- The domain knows about `@JsonProperty("frequency")` — this should be in an infrastructure layer

### 2.5 Current API Surface

| Method | Path | Status |
|---|---|---|
| POST | `/api/v1/auth/register` | Implemented |
| POST | `/api/v1/auth/login` | Implemented |
| POST | `/api/v1/auth/refresh-token` | Implemented |
| POST | `/api/v1/auth/logout` | Implemented |
| POST | `/api/v1/customers` | Implemented |
| GET | `/api/v1/customers/{id}` | Implemented |
| PATCH | `/api/v1/customers/{id}` | Implemented |
| DELETE | `/api/v1/customers/{id}` | Implemented |
| POST | `/api/v1/payments/billing` | Implemented (idempotency key from `X-Idempotency-Key` header) |
| POST | `/api/v1/products` | Implemented |
| GET | `/api/v1/products/{id}` | Implemented |
| GET | `/api/v1/products/sku/{sku}` | Implemented |
| GET | `/api/v1/products` | Implemented |
| GET | `/api/v1/products/available` | Implemented |
| PUT | `/api/v1/products/{id}` | Implemented |
| DELETE | `/api/v1/products/{id}` | Implemented |
| POST | `/api/v1/products/{id}/stock/decrease` | Implemented |
| POST | `/api/v1/products/{id}/stock/increase` | Implemented |

### 2.6 Database Schema (Flyway Migrations)

| Migration | Content |
|---|---|
| V1 | `roles`, `users`, `user_roles`, `refresh_tokens` |
| V2 | `customers` |
| V3 | `orders`, `order_items` (later dropped in V10) |
| V4 | `payment_intents`, `payment_transactions` |
| V5 | `risk_analyses` |
| V6 | `idempotency_keys` |
| V7 | `outbox_events` (table exists, never used) |
| V8 | `audit_logs` (table exists, never used) |
| V9 | Default `USER` role seed |
| V10 | Drops `orders` and `order_items` tables |
| V11 | `products` table |
| V12 | Recreates `payment_transactions` (decoupled from old FK chain) |
| V13 | Add `external_id` column and index to `payment_intents` |

### 2.7 Infrastructure

| Component | Details |
|---|---|
| **Database** | MySQL 8.0 via R2DBC (reactive) |
| **Cache/Session** | Redis 7.2 (configured but not yet used in code) |
| **Migrations** | Flyway, auto-run on startup |
| **External API** | AbacatePay sandbox (`POST /v1/billing/create` only) |
| **Observability** | Actuator + Micrometer + Prometheus (meter registries present, no custom metrics) |
| **Container** | `docker-compose.yml` with MySQL + Redis + app; multi-stage `Dockerfile` (Maven build + JRE runtime) |
| **CI/CD** | None (no `.github/workflows/`) |

---

## 3. Known Issues (as of 2026-05-28 — Phase 1)

**Resolved by Phase 1:**
| ID | Severity | Description | Status |
|---|---|---|---|
| BUG-01 | Critical | `createBilling(request)` called without `idempotencyKey` parameter | Resolved: extracted from `X-Idempotency-Key` header |
| BUG-02 | High | `IllegalArgumentException` for duplicate document | Resolved: replaced with `DuplicateDocumentException` |
| BUG-03 | High | `IllegalArgumentException` for duplicate SKU | Resolved: replaced with `DuplicateSkuException` |
| BUG-04 | High | `data.getAmount()` deprecated by AbacatePay API | Resolved: computed from `PaymentIntent.amount` |
| BUG-05 | Medium | Error messages in Portuguese | Resolved: changed to English |
| STUB-01 | Info | Empty `PaymentTransactionService.java` | Removed |
| STUB-02 | Info | Empty `PaymentWebhookHandler.java` | Removed |
| STUB-03 | Info | Empty V13 migration | Populated with `external_id` column + index |
| STUB-04 | Info | Placeholder Dockerfile | Replaced with multi-stage build |
| GAP-03 | Info | `refreshTokenExpirationDate` hardcoded at bean creation | Resolved: computed lazily in `saveRefreshToken()` |
| GAP-04 | Info | Refresh token expiration hardcoded `86400L` | Resolved: configurable via `JWT_REFRESH_TOKEN_EXPIRATION` |
| GAP-05 | Info | `refreshTokenExpirationDate` field initializer | Resolved: removed field, computed at call time |

**Remaining:**

| ID | Severity | Description |
|---|---|---|
| GAP-01 | Info | Zero test coverage (single placeholder test class) |
| GAP-02 | Info | No domain events despite `outbox_events` table existing |

**API paths updated:**
- All endpoints moved from `/api/...` to `/api/v1/...`
- Auth: `/api/v1/auth/**` (public)
- Customers: `/api/v1/customers/**`
- Payments: `/api/v1/payments/billing`
- Products: `/api/v1/products/**`
- SecurityConfig and JwtAuthWebFilter updated accordingly

---

## 4. Target Architecture

### 4.1 Architecture Style

**Hexagonal (Ports & Adapters) + Domain-Driven Design**

```
checkout/
├── PaymentServiceApplication.java
├── common/                        # Shared kernel across bounded contexts
│   ├── domain/                    # Base value objects, domain events, aggregate root
│   │   ├── valueobject/           # Money, Currency, EntityId
│   │   ├── event/                 # DomainEvent, EventBus (port)
│   │   └── AggregateRoot.java
│   └── infrastructure/            # Shared infrastructure: logging, correlation ID, error model
├── boundedcontext/
│   ├── user/                      # Auth bounded context
│   │   ├── domain/                # User aggregate, UserId, UserRegistered event
│   │   ├── application/           # UserService, AuthenticationService (use cases)
│   │   ├── infrastructure/        # R2DBC repos, JWT impl, BCrypt adapter
│   │   └── api/                   # REST controller, DTOs
│   ├── customer/                  # Customer bounded context
│   │   ├── domain/                # Customer aggregate, Document VO, CustomerId
│   │   ├── application/
│   │   ├── infrastructure/
│   │   └── api/
│   ├── product/                   # Product bounded context
│   │   ├── domain/                # Product aggregate, SKU VO, Money VO
│   │   ├── application/
│   │   ├── infrastructure/
│   │   └── api/
│   ├── payment/                   # Payment bounded context (core)
│   │   ├── domain/                # PaymentIntent aggregate, PaymentStatus, PaymentMethod VO, domain events
│   │   ├── application/           # CreateBillingUseCase, ProcessPaymentUseCase, RefundPaymentUseCase
│   │   ├── infrastructure/
│   │   │   ├── persistence/       # R2DBC repositories
│   │   │   ├── provider/          # AbacatePay HTTP client (adapter)
│   │   │   ├── adapter/           # Anti-Corruption Layer (provider DTO ↔ domain)
│   │   │   └── messaging/         # Outbox publisher, domain event dispatcher
│   │   └── api/                   # REST controller, DTOs, webhook handler
│   └── risk/                      # Future: Risk Analysis bounded context
├── config/                        # Spring configuration
└── integration/                   # Integration tests
```

### 4.2 Target Domain Model

#### PaymentIntent Aggregate (core of the system)

```
PaymentIntent (Aggregate Root)
├── id: PaymentIntentId
├── amount: Money
├── status: PaymentStatus (PENDING | PROCESSING | APPROVED | DENIED | REFUNDED)
├── paymentMethods: List<PaymentMethod>
├── idempotencyKey: IdempotencyKey
├── customerId: CustomerId
├── products: List<BillingProduct>
├── createdAt: Instant
├── updatedAt: Instant
│
├── PaymentTransaction (child entity, 1:N)
│   ├── id: TransactionId
│   ├── externalId: ExternalBillingId
│   ├── status: String (provider status)
│   └── processedAt: Instant
│
├── Behaviors:
│   ├── process(externalId, providerStatus) → PaymentProcessed event
│   ├── approve() → PaymentApproved event
│   ├── deny(reason) → PaymentDenied event
│   ├── refund() → PaymentRefunded event
│   └── canTransitionTo(PaymentStatus) → boolean
│
└── Invariants:
    ├── Cannot process if not PENDING
    ├── Cannot approve if not PROCESSING
    ├── Cannot refund if not APPROVED
    ├── Cannot deny a REFUNDED payment
    └── Idempotency key must be unique
```

#### Value Objects to Introduce

| VO | Replaces | Contains |
|---|---|---|
| `Money` | `BigDecimal` + `String currency` field | `BigDecimal amount`, `Currency currency`, factory methods (`ofCents(int)`, `zero()`, `add/subtract`) |
| `PaymentStatus` | raw `String` status field | Enum with transition rules (`canTransitionTo()`, `PENDING`, `PROCESSING`, `APPROVED`, `DENIED`, `REFUNDED`) |
| `PaymentIntentId` | raw `Long id` | Wrapper with validation |
| `CustomerId` | raw `Long` | Wrapper with validation |
| `ProductId` | raw `Long` | Wrapper with validation |
| `IdempotencyKey` | raw `String` | UUID validation, `of(UUID)` factory |
| `ExternalBillingId` | raw `String` | Provider-specific ID wrapper |
| `SKU` | raw `String` | Validation, immutability |
| `Document` | Already exists but should move to `customer/domain` as proper VO | `documentType`, `documentNumber` with CPF/CNPJ validation |

#### Domain Events

| Event | Triggers | Handlers |
|---|---|---|
| `PaymentInitiated` | PaymentIntent created, idempotency checked | Audit log, metrics |
| `PaymentProcessed` | Provider returns success/failure | Risk analysis, notification |
| `PaymentApproved` | Provider confirms payment | Notification, order fulfillment |
| `PaymentDenied` | Provider rejects payment | Notification, audit |
| `PaymentRefunded` | Refund requested and processed | Notification, audit |
| `UserRegistered` | New user created | Welcome notification |
| `CustomerCreated` | New customer KYC passed | Audit |

### 4.3 Target API Surface (v1)

All endpoints under `/api/v1/`:

| Method | Path | Context |
|---|---|---|
| POST | `/api/v1/auth/register` | User |
| POST | `/api/v1/auth/login` | User |
| POST | `/api/v1/auth/refresh-token` | User |
| POST | `/api/v1/auth/logout` | User |
| POST | `/api/v1/customers` | Customer |
| GET | `/api/v1/customers/{id}` | Customer |
| PATCH | `/api/v1/customers/{id}` | Customer |
| DELETE | `/api/v1/customers/{id}` | Customer |
| POST | `/api/v1/products` | Product |
| GET | `/api/v1/products/{id}` | Product |
| PUT | `/api/v1/products/{id}` | Product |
| DELETE | `/api/v1/products/{id}` | Product |
| POST | `/api/v1/payments/billing` | Payment — **Idempotency-Key header** |
| GET | `/api/v1/payments/billing/{id}` | Payment — retrieve billing |
| GET | `/api/v1/payments/billing/list` | Payment — list all billings |
| POST | `/api/v1/payments/billing/{id}/cancel` | Payment — cancel billing |
| POST | `/api/v1/payments/billing/webhook` | Payment — provider webhook handler |

### 4.4 Anti-Corruption Layer Design

```
[Controller] → [UseCase/AppService] → [DomainService] → [Port: PaymentProviderPort]
                                                              │
                                          ┌───────────────────▼───────────────────┐
                                          │  Infrastructure Adapter               │
                                          │  ┌─────────────────────────────────┐  │
                                          │  │ AbacatePayClient                │  │ ← WebClient
                                          │  │ (implements PaymentProviderPort) │  │
                                          │  └───────────────┬─────────────────┘  │
                                          │                  │                     │
                                          │  ┌───────────────▼─────────────────┐  │
                                          │  │ AbacatePayAdapter               │  │ ← ACL
                                          │  │ extRequest → domain objects     │  │
                                          │  │ extResponse → domain objects    │  │
                                          │  └─────────────────────────────────┘  │
                                          └──────────────────────────────────────┘
```

The domain layer knows only the `PaymentProviderPort` interface. The infrastructure layer implements it. The ACL (`AbacatePayAdapter`) is a pure translation layer with zero domain logic. External DTOs (`*ExtDto`) live in `infrastructure/provider/` only.

### 4.5 Resilience Targets

| Pattern | Tool | Application |
|---|---|---|
| Circuit Breaker | Resilience4j | `PaymentProviderPort` calls |
| Retry (exponential backoff) | Resilience4j | Transient AbacatePay errors |
| Rate Limiting | Redis + custom filter | `/api/v1/auth/login`, `/api/v1/auth/register` |
| Timeout | WebClient config (already exists) | AbacatePay calls |
| Correlation ID | `X-Correlation-ID` header | All requests, logged + propagated to AbacatePay |

### 4.6 Observability Targets

| Metric | Type |
|---|---|
| `payment_intents_created_total` | Counter |
| `payment_intents_approved_total` | Counter |
| `payment_intents_denied_total` | Counter |
| `payment_provider_call_duration_seconds` | Histogram (P50, P95, P99) |
| `payment_provider_errors_total` | Counter (by type) |
| `idempotency_key_hits_total` | Counter |
| `active_users_count` | Gauge |

### 4.7 Testing Strategy

| Layer | Tool | Coverage Target |
|---|---|---|
| Domain (aggregates, VOs) | JUnit 5 + AssertJ | 100% (no mocking needed) |
| Application (use cases) | JUnit 5 + Mockito | ≥80% |
| Infrastructure (adapters) | Integration with Testcontainers | ≥70% |
| API (controllers) | WebTestClient + Testcontainers | ≥70% |
| E2E (critical paths) | Testcontainers, full stack | Happy path coverage |

---

## 5. Technology Stack (Current + Target)

| Category | Current | Target |
|---|---|---|
| Runtime | Java 17 | Java 21 (LTS) |
| Framework | Spring Boot 3.5.3, WebFlux | Same |
| Database | MySQL 8.0 + R2DBC | Same |
| Cache | Redis 7.2 (unused) | Redis 7.2 (rate limiting, distributed locks) |
| Security | Spring Security + jjwt 0.12.3 | Same |
| Resilience | None | Resilience4j (Circuit Breaker, Retry, TimeLimiter) |
| Testing | JUnit 5 (1 placeholder test) | JUnit 5, AssertJ, Mockito, Testcontainers, WebTestClient |
| Observability | Actuator + Micrometer | Same + custom meters + structured JSON logging |
| CI/CD | None | GitHub Actions (build, test, quality gate, container push) |
| Container | Placeholder Dockerfile | Multi-stage `Dockerfile` (build: maven, run: eclipse-temurin:21-jre-alpine) |
| Documentation | Manually maintained OpenAPI | Same, kept in sync with implementation |
| API Versioning | None | URL prefix `/api/v1/` |

---

## 6. Constraints

1. **Monolith first.** No microservices until domain model is solid.
2. **Reactive all the way.** No blocking calls — everything is `Mono<T>` / `Flux<T>`.
3. **No ORM.** R2DBC is raw SQL mapping; rich domain models are assembled in repositories/mappers.
4. **AbacatePay sandbox only.** No real money moves. Test API key is safe to commit (dev-only).
5. **Portuguese domain terms** (CPF, CNPJ, Boleto) are kept as-is — they're domain concepts.
6. **Every change updates SPEC.md.** This file is the source of truth for current state.
7. **Backward compatibility NOT required.** This is a portfolio project, not production. Breaking changes are acceptable.

---

## 7. Document Versioning

| Version | Date | Author | Changes |
|---|---|---|---|
| 1.0 | 2026-05-28 | AI + dev | Initial specification: current state audit + target architecture |
| 1.1 | 2026-05-28 | AI + dev | Phase 1 complete: 5 bugs fixed, 5 tech foundation tasks (Dockerfile, API v1, cleanup, V13 migration) |

---

*End of SPEC.md — next update with Phase 2 (Value Objects).*
