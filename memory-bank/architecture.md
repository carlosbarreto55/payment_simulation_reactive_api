# Architecture

## Primary Pattern: Layered Domain-Driven Design (DDD)

The project blends **Service-Oriented Architecture (SOA)**, **Layered Architecture**, and **Domain-Driven Design (DDD)**.

## Layer Structure

### Legacy Domain Package (`checkout.domain.*`)

```
┌─────────────────────────────────────────────────────────┐
│                 REST Controllers (interface)              │
│         domain/{customer,payment,product,user}/controller │
├─────────────────────────────────────────────────────────┤
│                  Services (application)                   │
│          domain/{customer,payment,product,user}/service   │
├─────────────────────────────────────────────────────────┤
│              Entities + Value Objects (domain)            │
│          domain/{customer,payment,product,user}/entity    │
├─────────────────────────────────────────────────────────┤
│            Repositories + Clients (infrastructure)        │
│         domain/*/repository  +  domain/client + config/   │
└─────────────────────────────────────────────────────────┘
```

### New Bounded Context Package (`checkout.boundedcontext.*`)

The project is incrementally migrating toward a stricter **Domain-Driven Design (DDD)** structure using `boundedcontext/` paths for new domain value objects and aggregates. This coexists with the existing `domain/` layout during the transition.

```
checkout/
├── boundedcontext/
│   ├── payment/domain/       # Payment VO/enums (PaymentMethod, IdempotencyKey, etc.)
│   └── customer/domain/      # Customer VO/enums (Document, etc.)
└── domain/
    ├── customer/             # Legacy customer controllers, services, entities, DTOs
    ├── payment/              # Legacy payment controllers, services, entities, DTOs
    └── ...
```

- **Value Objects & Enums** for new DDD modeling live under `checkout.boundedcontext.<context>.domain.*`
- **Controllers, Services, Repositories, and DTOs** remain under `checkout.domain.*` until explicitly refactored in later phases
- The two structures are intentionally transitional; old enums are deprecated but kept until Phase 3 refactor

## Key Architectural Decisions

### Reactive by Default
- All controllers return `Mono<T>` or `Flux<T>`
- All repositories extend `ReactiveCrudRepository`
- Database access via R2DBC (non-blocking)
- HTTP calls via WebClient (non-blocking)
- No blocking calls anywhere in the hot path

### Domain Separation
Each domain (user, customer, payment, product) is fully isolated:
- Own entity classes with domain behavior methods
- Own repositories extending `ReactiveCrudRepository`
- Own service classes with business logic
- Own controllers exposing REST endpoints
- Own DTOs for request/response contracts

### Infrastructure Configuration
- `PaymentProviderConfig` — Configures WebClient for PSP integration with connection pooling, TLS, compression
- `R2dbcConfig` — Enables R2DBC repositories and reactive transaction management
- `SecurityConfig` — Spring Security WebFlux configuration with JWT filter

### Patterns Implemented
1. **Idempotency** — Payment operations use `X-Idempotency-Key` header; duplicate keys are rejected via `DuplicateIdempotencyKeyException`
2. **JWT Token Rotation** — Access tokens (5min) + Refresh tokens (24h); old refresh token revoked on rotation
3. **Global Exception Handling** — `@RestControllerAdvice` with `GlobalExceptionHandler` mapping 20+ domain exceptions to structured error responses
4. **Outbox Pattern** — Schema ready (`outbox_events` table), service not yet implemented
5. **RBAC** — Role-based access with ownership validation (CUSTOMER can only access own resources; SUPPORT/SYSTEM can access all)

### Security Architecture
```
Request → JwtAuthWebFilter → extracts Bearer token
  → validates JWT via JwtService
  → creates JwtAuthToken (userId, email, roles)
  → sets SecurityContext
  → reaches controller (with @PreAuthorize checks)
```

### Payment Flow
```
POST /api/payments/billing (X-Idempotency-Key)
  → Idempotency check (reject if duplicate)
  → Calculate total amount from product items
  → Persist PaymentIntent (status: PENDING)
  → Map to external DTO
  → Call PSP via WebClient (PaymentServiceClient)
  → Persist PaymentTransaction (with external ID)
  → Update PaymentIntent (status: PROCESSING or DENIED)
  → Return CreateBillingResponseDto
```

## Current Limitations / Technical Debt
- Orders module was removed (V10 migration); payment operates independently
- `PaymentMapper.toPaymentResponseDto()` uses deprecated `getAmount()` instead of calculating from product totals
- Dockerfile is a placeholder (`top -b`), not running the actual app
- Public path handling inconsistent between `SecurityConfig` and `JwtAuthWebFilter`
- Port mismatch: code runs on 38080, docs/readme reference 8080
