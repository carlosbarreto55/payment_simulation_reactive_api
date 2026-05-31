# PLAN.md — Implementation Plan

> Tasks are ordered by dependency. Each task is atomic, testable, and updates SPEC.md on completion.

---

## Legend

| Prefix | Meaning |
|---|---|
| `FIX-` | Bug fix |
| `TECH-` | Technical foundation (must come before domain changes) |
| `DDD-` | DDD transformation (value objects, aggregates, events) |
| `DOM-` | Domain behavior & use cases |
| `INFRA-` | Infrastructure (resilience, observability, CI/CD) |
| `API-` | API layer (controllers, DTOs, versioning) |
| `TEST-` | Testing |
| `DOC-` | Documentation updates |

---

## Progress Summary

> Updated automatically by the Orchestrator after each task completes.

| Phase | Total | Completed | Remaining | Status |
|---|---|---|---|---|
| Phase 1 — Bug Fixes & Tech Foundation | 10 | 9 | 1 | 🔄 In Progress |
| Phase 2 — DDD Value Objects | 9 | 9 | 0 | ✅ Complete |
| Phase 3 — Aggregates & Domain Services | 7 | 7 | 0 | ✅ Complete |
| Phase 4 — Infrastructure & ACL | 10 | 0 | 10 | ⬜ Not Started |
| Phase 5 — Controllers & API | 7 | 0 | 7 | ⬜ Not Started |
| Phase 6 — Testing | 10 | 2 | 8 | 🔄 In Progress |
| Phase 7 — DevOps & Docs | 6 | 0 | 6 | ⬜ Not Started |
| **Overall** | **59** | **27** | **32** | 🔄 **~46%** |

---

## Phase 1 — Critical Bug Fixes & Technical Foundation

### TASK FIX-01: Fix PaymentController missing idempotencyKey parameter
- **Status**: ✅ Completed
- **File**: `domain/payment/controller/PaymentController.java`
- **What**: Extract `X-Idempotency-Key` header in controller, pass to service method
- **Verification**: Compiles; curl with and without header returns appropriate errors
- **SPEC**: Update 3. Known Issues: mark BUG-01 as resolved

### TASK FIX-02: Replace IllegalArgumentException with custom exceptions
- **Status**: ✅ Completed
- **Files**: `CustomerService.java:41`, `ProductService.java:30`
- **What**: Create `DuplicateDocumentException` and `DuplicateSkuException` (extending `BusinessRuleException`), replace `IllegalArgumentException` throws
- **Verification**: Compiles; exception handler returns proper HTTP 409
- **SPEC**: Update 3. Known Issues: mark BUG-02, BUG-03 as resolved

### TASK FIX-03: Fix deprecated amount usage in PaymentMapper
- **Status**: ✅ Completed
- **File**: `domain/payment/mapper/PaymentMapper.java:67`
- **What**: Remove `data.getAmount()` usage. Compute `amountInCents` from `PaymentIntent.amount` instead. The response DTO `CreateBillingResponseDto` should derive amount from the domain object, not the deprecated provider field.
- **Verification**: Response still contains valid `amountInCents`
- **SPEC**: Update 3. Known Issues: mark BUG-04 as resolved

### TASK FIX-04: Fix hardcoded refresh token expiration
- **Status**: ✅ Completed
- **Files**: `JwtService.java:35`, `AuthenticationService.java:38`
- **What**: (a) Move `refreshTokenExpirationTime` to `application.properties` as `JWT_REFRESH_TOKEN_EXPIRATION`. (b) Fix `refreshTokenExpirationDate` to compute `LocalDateTime.now().plusSeconds(...)` lazily instead of at bean creation.
- **Verification**: Refresh tokens honor config value, not hardcoded 86400
- **SPEC**: Update 3. Known Issues: mark GAP-03, GAP-04, GAP-05 as resolved

### TASK FIX-05: Fix error messages to English
- **Status**: ✅ Completed
- **File**: `common/exception/GlobalExceptionHandler.java:70,88`
- **What**: Change "Erro de validação" → "Validation error" and "Erro interno do servidor" → "Internal server error"
- **Verification**: Error responses have English messages
- **SPEC**: Update 3. Known Issues: mark BUG-05 as resolved

### TASK TECH-01: Add proper Dockerfile
- **Status**: ✅ Completed
- **File**: `payment_service/Dockerfile`
- **What**: Multi-stage build: (1) `maven:3.9-eclipse-temurin-21` for `mvn package -DskipTests`, (2) `eclipse-temurin:21-jre-alpine` for runtime. Copy JAR, set ENTRYPOINT.
- **Verification**: `docker build -t payment-service .` succeeds
- **SPEC**: Update 3. Known Issues: mark STUB-04 as resolved

### TASK TECH-02: Upgrade Java to 21 (LTS)
- **Status**: ⬜ Pending
- **Files**: `pom.xml` (`<java.version>21</java.version>`), `Dockerfile`
- **What**: Update Maven compiler properties, Dockerfile base images
- **Verification**: `./mvnw clean compile` passes
- **SPEC**: Update 5. Technology Stack: Java 17 → 21

### TASK TECH-03: Add API versioning prefix
- **Status**: ✅ Completed
- **Files**: All controllers, `SecurityConfig.java`, `JwtAuthWebFilter.java`, `api-spec.yaml`
- **What**: Prefix all endpoints with `/v1` (e.g. `/api/v1/auth/login`). Update security config public paths. Update OpenAPI spec.
- **Verification**: All endpoints respond at `/api/v1/...`; `/api/...` returns 404
- **SPEC**: Update 2.5 Current API Surface, 4.3 Target API Surface

### TASK TECH-04: Delete empty/stub files
- **Status**: ✅ Completed
- **Files**: `PaymentTransactionService.java`, `PaymentWebhookHandler.java`, `V13__add_external_id_to_payment_intents.sql`
- **What**: Remove empty files that provide no value
- **Verification**: Compiles; no references to deleted classes
- **SPEC**: Update 3. Known Issues: mark STUB-01, STUB-02, STUB-03 as resolved

### TASK TECH-05: Populate V13 migration with actual SQL
- **Status**: ✅ Completed
- **File**: `V13__add_external_id_to_payment_intents.sql`
- **What**: Add `external_id VARCHAR(255)` column and index to `payment_intents` table
- **Verification**: `./mvnw flyway:migrate` succeeds on clean DB
- **SPEC**: Update 2.6 Database Schema: V13 now has content

---

## Phase 2 — DDD Foundation: Value Objects

### TASK DDD-01: Create Money value object
- **Status**: ✅ Completed
- **File**: `common/domain/valueobject/Money.java`
- **What**: Immutable record with `BigDecimal amount`, `String currency`. Factory methods: `ofCents(int)`, `zero()`, `ofBrl(BigDecimal)`. Methods: `add(Money)`, `subtract(Money)`, `isGreaterThan(Money)`, `toCents()`, `equals/hashCode`.
- **Verification**: Unit test for equality, arithmetic, immutability, edge cases (null, negative)
- **SPEC**: Update 4.2 Value Objects: mark Money as implemented

### TASK DDD-02: Create PaymentStatus value object
- **Status**: ✅ Completed
- **File**: `boundedcontext/payment/domain/PaymentStatus.java`
- **What**: Enum with states: `PENDING`, `PROCESSING`, `APPROVED`, `DENIED`, `REFUNDED`. Method: `canTransitionTo(PaymentStatus target)` returning boolean with valid transitions. Method: `isTerminal()`.
- **Verification**: Unit test for all valid transitions, all invalid transitions
- **SPEC**: Update 4.2 Value Objects: mark PaymentStatus as implemented

### TASK DDD-03: Create PaymentMethod value object
- **Status**: ✅ Completed
- **File**: `boundedcontext/payment/domain/PaymentMethod.java`
- **What**: Replace raw `List<String>` and existing `PaymentMethod` enum. Enum with `PIX`, `CARD`. Static factory `fromAbacatePay(String)`. Method `toAbacatePayString()`. List has `List<PaymentMethod>` validation (1-2 elements, unique).
- **Verification**: Unit test for all valid/invalid conversions
- **SPEC**: Update 4.2: PaymentMethod VO implemented

### TASK DDD-04: Create PaymentIntentId value object
- **Status**: ✅ Completed
- **File**: `boundedcontext/payment/domain/PaymentIntentId.java`
- **What**: Immutable record wrapping `Long`. Factory `PaymentIntentId.of(Long)`, validation (non-null, positive). `equals/hashCode/toString`.
- **Verification**: Unit test for null rejection, equality
- **SPEC**: Update 4.2: PaymentIntentId VO implemented

### TASK DDD-05: Create IdempotencyKey value object
- **Status**: ✅ Completed
- **File**: `boundedcontext/payment/domain/IdempotencyKey.java`
- **What**: Immutable record wrapping `String`. Factory `IdempotencyKey.of(String)` with non-blank validation. `equals/hashCode/toString`.
- **Verification**: Unit test for null/blank rejection, equality
- **SPEC**: Update 4.2: IdempotencyKey VO implemented

### TASK DDD-06: Create ExternalBillingId value object
- **Status**: ✅ Completed
- **File**: `boundedcontext/payment/domain/ExternalBillingId.java`
- **What**: Immutable record wrapping `String`. Factory `ExternalBillingId.of(String)` with non-blank validation.
- **Verification**: Unit test
- **SPEC**: Update 4.2: ExternalBillingId VO implemented

### TASK DDD-07: Create CustomerId value object
- **Status**: ✅ Completed
- **File**: `boundedcontext/customer/domain/CustomerId.java`
- **What**: Immutable record wrapping `Long`. Same conventions as PaymentIntentId.
- **Verification**: Unit test
- **SPEC**: Update 4.2: CustomerId VO implemented

### TASK DDD-08: Create SKU value object
- **Status**: ✅ Completed
- **File**: `boundedcontext/product/domain/SKU.java`
- **What**: Immutable record wrapping `String`. Validation: non-blank, length 3-50, uppercase alphanumeric.
- **Verification**: Unit test for validation
- **SPEC**: Update 4.2: SKU VO implemented

### TASK DDD-09: Refactor Document into proper value object
- **Status**: ✅ Completed
- **File**: Move `Document.java` to `boundedcontext/customer/domain/Document.java`
- **What**: Make immutable (remove `@Data`, use record or `@Value`). Add CPF/CNPJ format validation in constructor. Add `Document.from(DocumentType, String)` factory with validation.
- **Verification**: Unit test for CPF/CNPJ format validation
- **SPEC**: Update 4.2: Document VO refactored

---

## Phase 3 — DDD Core: Aggregate Roots & Domain Services

### TASK DDD-10: Create DomainEvent base class and EventBus port
- **Status**: ✅ Completed (2026-05-30)
- **Files**: `common/domain/event/DomainEvent.java`, `common/domain/event/EventBus.java`
- **What**: Base interface `DomainEvent` with `eventId`, `occurredAt`, `aggregateId`. Port interface `EventBus` with `publish(DomainEvent): Mono<Void>`.
- **Verification**: Compiles; interfaces are defined
- **SPEC**: Update 4.1 Target Architecture: domain events foundation

### TASK DDD-11: Rebuild PaymentIntent as aggregate root
- **Status**: ✅ Completed (2026-05-30)
- **File**: `boundedcontext/payment/domain/PaymentIntent.java`
- **What**: Remove `@Data @Builder @NoArgsConstructor @AllArgsConstructor`. Use `@Getter` only. Constructor with invariant validation. Use `PaymentIntentId`, `Money`, `PaymentStatus`, `IdempotencyKey`, `PaymentMethod` VOs. Add `PaymentTransaction` as child entity (1:N list). Add methods:
  - `PaymentIntent.initiate(idempotencyKey, amount, methods, customerId, products)` → static factory, returns new PaymentIntent in PENDING state, raises `PaymentInitiated`
  - `process(externalId, providerStatus)` → transitions PENDING→PROCESSING, adds transaction, raises `PaymentProcessed`
  - `approve()` → transitions PROCESSING→APPROVED, raises `PaymentApproved`
  - `deny(reason)` → transitions PROCESSING→DENIED, records failure reason, raises `PaymentDenied`
  - `refund()` → transitions APPROVED→REFUNDED, raises `PaymentRefunded`
- **Verification**: Unit tests for all state transitions, invariant violations, domain event emission
- **SPEC**: Update 4.2 PaymentIntent Aggregate, mark implemented

### TASK DDD-12: Rebuild PaymentTransaction as child entity
- **Status**: ✅ Completed (2026-05-30)
- **File**: `boundedcontext/payment/domain/PaymentTransaction.java`
- **What**: Remove `@Data`. Struct with `ExternalBillingId`, `String providerStatus`, `Instant processedAt`. Factory: `PaymentTransaction.record(externalId, providerStatus)`. Immutable after creation. Moved inside `PaymentIntent` as static inner or top-level with package-private constructor.
- **Verification**: Unit test for immutability
- **SPEC**: Update 4.2: PaymentTransaction implemented

### TASK DDD-13: Create domain events for Payment context
- **Status**: ✅ Completed (2026-05-30)
- **Files**: `boundedcontext/payment/domain/event/PaymentInitiated.java`, `PaymentProcessed.java`, `PaymentApproved.java`, `PaymentDenied.java`, `PaymentRefunded.java`
- **What**: Each event is a record implementing `DomainEvent`. Carries relevant data (paymentIntentId, amount, timestamp, provider status, reason).
- **Verification**: Compiles; events carry correct payload
- **SPEC**: Update 4.2 Domain Events: mark Payment events as implemented

### TASK DDD-14: Create PaymentProviderPort interface
- **Status**: ✅ Completed (2026-05-30)
- **File**: `boundedcontext/payment/domain/port/PaymentProviderPort.java`
- **What**: Interface in domain layer: `Mono<ExternalBillingResponse> createBilling(CreateBillingDomainRequest)`, `Mono<ExternalBillingResponse> getBilling(ExternalBillingId)`, `Mono<ExternalBillingResponse> cancelBilling(ExternalBillingId)`, `Mono<List<ExternalBillingResponse>> listBillings()`. Domain request/response objects (not external DTOs).
- **Verification**: Compiles; interface defines domain contracts
- **SPEC**: Update 4.4 ACL Design: port interface added

### TASK DDD-15: Create payment domain service
- **Status**: ✅ Completed (2026-05-30)
- **File**: `boundedcontext/payment/domain/PaymentDomainService.java`
- **What**: Stateless domain service for operations that don't belong to a single aggregate: `Mono<PaymentIntent> initiatePayment(CreateBillingRequest, IdempotencyKey)` — checks idempotency, creates PaymentIntent, publishes event. Uses `PaymentProviderPort` and `PaymentIntentRepository` (domain repository interface, not Spring Data).
- **Verification**: Unit test with mocked ports
- **SPEC**: Update 4.1: PaymentDomainService added

### TASK DDD-16: Define domain repository interfaces
- **Status**: ✅ Completed (2026-05-30)
- **Files**: `boundedcontext/payment/domain/repository/PaymentIntentRepository.java`, `PaymentTransactionRepository.java`
- **What**: These are DOMAIN-layer interfaces (not Spring Data). Define: `save(PaymentIntent): Mono<PaymentIntent>`, `findById(PaymentIntentId): Mono<PaymentIntent>`, `existsByIdempotencyKey(IdempotencyKey): Mono<Boolean>`, `findByIdWithTransactions(PaymentIntentId): Mono<PaymentIntent>`.
- **Verification**: Compiles; Spring Data repos will implement these in infrastructure
- **SPEC**: Update 4.1: domain repository interfaces added

---

## Phase 4 — Infrastructure & Anti-Corruption Layer

### TASK INFRA-01: Create AbacatePay HTTP client adapter
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/infrastructure/provider/AbacatePayClient.java`
- **What**: Move `PaymentServiceClient` here. Rename to `AbacatePayClient`. Implements `PaymentProviderPort`. Uses existing `paymentProviderWebClient` bean. Add endpoints: GET `/v1/billing/list`, GET `/v1/billing/{id}`, POST `/v1/billing/{id}/cancel`. Add correlation ID propagation via `X-Correlation-ID` header. Delete old `domain/paymentProvider/PaymentServiceClient.java`.
- **Verification**: WebClient calls to sandbox succeed (can test with curl or integration test)
- **SPEC**: Update 4.4: AbacatePayClient implemented

### TASK INFRA-02: Create Anti-Corruption Layer adapter
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/infrastructure/provider/AbacatePayAdapter.java`
- **What**: Pure translation layer. Methods: `toExternalRequest(CreateBillingDomainRequest) → CreateBillingRequestExtDto`, `toDomain(ExternalBillingId, CreateBillingResponseExtDto) → ExternalBillingResponse` (domain object), `toDomainBillingList(List<...ExtDto>) → List<ExternalBillingResponse>`. Zero business logic.
- **Verification**: Unit test with sample JSON → domain object mappings
- **SPEC**: Update 4.4: ACL implemented

### TASK INFRA-03: Move external DTOs to infrastructure
- **Status**: ⬜ Pending
- **Files**: `CreateBillingRequestExtDto.java`, `CreateBillingResponseExtDto.java`, `CustomerExtDto.java`, `ProductExtDto.java`
- **What**: Move from `domain/payment/dto/` to `boundedcontext/payment/infrastructure/provider/dto/`. Update imports. Delete the mapper `PaymentMapper.java` (replaced by ACL).
- **Verification**: Compiles; no domain code imports `*ExtDto`
- **SPEC**: Update 4.4: external DTOs relocated

### TASK INFRA-04: Implement R2DBC PaymentIntentRepository
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/infrastructure/persistence/R2dbcPaymentIntentRepository.java`
- **What**: Implements domain `PaymentIntentRepository` interface. Uses Spring Data R2DBC under the hood. Maps between DB rows and domain aggregate (assembles `PaymentIntent` with child `PaymentTransaction` list). Handles `PaymentStatus`, `Money`, `PaymentIntentId` conversions.
- **Verification**: Integration test with Testcontainers MySQL
- **SPEC**: Update 4.1: infrastructure persistence implemented

### TASK INFRA-05: Separate controller DTOs from domain DTOs
- **Status**: ⬜ Pending
- **What**: Ensure each bounded context has `api/dto/` for HTTP contracts and domain objects are never leaked in API responses. Controllers return API DTOs, not domain objects.
- **Verification**: No domain entity/VO appears in controller method signatures or return types
- **SPEC**: Update 4.3: API DTO separation confirmed
---
---
## Phase 5— Testing

### TASK TEST-01: Unit tests for Money value object
- **Status**: ✅ Completed
- **File**: `common/domain/valueobject/MoneyTest.java`
- **What**: Test creation, `ofCents()`, `add()`, `subtract()`, `isGreaterThan()`, edge cases (overflow, null, zero)
- **Verification**: `./mvnw test -Dtest=MoneyTest`

### TASK TEST-02: Unit tests for PaymentStatus transitions
- **Status**: ✅ Completed
- **File**: `boundedcontext/payment/domain/PaymentStatusTest.java`
- **What**: Parametrized test: valid transitions pass, invalid transitions throw. All 5×5 combinations covered.
- **Verification**: `./mvnw test -Dtest=PaymentStatusTest`

### TASK TEST-03: Unit tests for PaymentIntent aggregate
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/domain/PaymentIntentTest.java`
- **What**: Test: `initiate()` creates in PENDING state; `process()` adds transaction and transitions to PROCESSING; `approve()` transitions to APPROVED; `deny()` records reason; `refund()` transitions to REFUNDED; double process throws; approve from PENDING throws; refund from PENDING throws; domain events raised correctly.
- **Verification**: `./mvnw test -Dtest=PaymentIntentTest`

### TASK TEST-04: Unit tests for PaymentDomainService
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/domain/PaymentDomainServiceTest.java`
- **What**: Test idempotency check (duplicate key returns existing intent), successful payment creation, provider failure handling
- **Verification**: `./mvnw test -Dtest=PaymentDomainServiceTest`

### TASK TEST-05: Unit tests for AbacatePayAdapter (ACL)
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/infrastructure/provider/AbacatePayAdapterTest.java`
- **What**: Test domain→external mapping, external→domain mapping, null handling, edge cases
- **Verification**: `./mvnw test -Dtest=AbacatePayAdapterTest`

### TASK TEST-06: Integration tests for PaymentRepository
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/infrastructure/persistence/PaymentIntentRepositoryImplTest.java`
- **What**: Testcontainers MySQL. Test save+find aggregate with child transactions. Test idempotency key check. Test entity-to-aggregate assembly.
- **Verification**: `./mvnw test -Dtest=PaymentIntentRepositoryImplTest`

### TASK TEST-07: Integration tests for PaymentController
- **Status**: ⬜ Pending
- **File**: `boundedcontext/payment/api/PaymentControllerTest.java`
- **What**: WebTestClient + Testcontainers. Test: create billing with idempotency key, get billing by ID, list billings, cancel billing, webhook handler, missing idempotency key returns 400, duplicate idempotency key returns 409, invalid billing ID returns 404.
- **Verification**: `./mvnw test -Dtest=PaymentControllerTest`

### TASK TEST-08: Integration tests for Auth flow
- **Status**: ⬜ Pending
- **File**: `boundedcontext/user/api/AuthControllerTest.java`
- **What**: Register → login → get access+refresh tokens → refresh → logout → login with same credentials. Test rate limiting returns 429. Test invalid credentials returns 401.
- **Verification**: `./mvnw test -Dtest=AuthControllerTest`

### TASK TEST-09: E2E test for full payment flow
- **Status**: ⬜ Pending
- **File**: `integration/PaymentFlowE2ETest.java`
- **What**: Register → create customer → create product → create billing → get billing → cancel billing. Full chain with Testcontainers. Mock AbacatePay responses via WireMock.
- **Verification**: `./mvnw test -Dtest=PaymentFlowE2ETest`

### TASK TEST-10: Delete placeholder test and ensure minimum coverage
- **Status**: ⬜ Pending
- **File**: Delete `test/java/teste/magalu/MagaluApplicationTests.java`
- **What**: Replace with real suite. Add JaCoCo configuration in `pom.xml` with minimum 70% line coverage.
- **Verification**: `./mvnw verify jacoco:report` passes coverage threshold

---

## Phase 7 — DevOps & Polish

### TASK DEVOPS-01: Create GitHub Actions CI pipeline
- **Status**: ⬜ Pending
- **File**: `.github/workflows/ci.yml`
- **What**: On PR/push to main: (1) checkout, (2) set up Java 21, (3) cache Maven deps, (4) `mvn verify` (tests + coverage), (5) `mvn spotless:check` for formatting, (6) `docker build`. Fail PR on test failure or coverage below threshold.
- **Verification**: Push → pipeline executes on GitHub

### TASK DEVOPS-02: Add code formatting with Spotless
- **Status**: ⬜ Pending
- **File**: `pom.xml` (spotless-maven-plugin)
- **What**: Enforce Google Java Format. `./mvnw spotless:apply` auto-formats. CI runs `spotless:check`.
- **Verification**: `./mvnw spotless:check` passes

### TASK DEVOPS-03: Enhance docker-compose.yml with health checks
- **Status**: ⬜ Pending
- **File**: `payment_service/docker-compose.yml`
- **What**: Add proper healthchecks for MySQL (`mysqladmin ping`), Redis (`redis-cli ping`), app (`curl actuator/health`). Add restart policies (`unless-stopped`). Update app service to build from proper Dockerfile.
- **Verification**: `docker-compose up -d` → all services healthy within 60s

### TASK DEVOPS-04: Add structured JSON logging
- **Status**: ⬜ Pending
- **Files**: `application.properties` (logstash-logback-encoder), `logback-spring.xml`
- **What**: Configure Logback to output JSON. Include fields: `timestamp`, `level`, `logger`, `message`, `correlationId`, `userId`. Console appender for local dev, file appender for production.
- **Verification**: Application logs appear as valid JSON lines

### TASK DOC-01: Update root README.md
- **Status**: ⬜ Pending
- **File**: `/README.md`
- **What**: Remove outdated "Magalu" branding. Update architecture diagram to reflect hexagonal + DDD. Update project structure to `boundedcontext/` layout. Add quickstart for Docker Compose. Add API documentation link.
- **Verification**: README accurately describes current project

### TASK DOC-02: Update SPEC.md final review
- **Status**: ⬜ Pending
- **File**: `/SPEC.md`
- **What**: Verify all sections reflect reality after all phases complete. Remove "Known Issues" that were fixed. Update version to 2.0.
- **Verification**: SPEC.md contains no stale information

---

## Dependency Graph

```
FIX-01 ──┐
FIX-02 ──┤
FIX-03 ──┼──→ TECH-01 ──→ TECH-02 ──→ TECH-03 ──→ TECH-04 ──→ TECH-05
FIX-04 ──┤
FIX-05 ──┘
                │
                ▼
        DDD-01 → DDD-02 → DDD-03 → DDD-04 → DDD-05 → DDD-06
        DDD-07 → DDD-08 → DDD-09
                │
                ▼
        DDD-10 → DDD-11 → DDD-12 → DDD-13
        DDD-14 → DDD-15 → DDD-16
                │
                ▼
        INFRA-01 → INFRA-02 → INFRA-03 → INFRA-04 → INFRA-05
        INFRA-06 → INFRA-07 → INFRA-08 → INFRA-09 → INFRA-10
                │
                ▼
        API-01 → API-02 → API-03 → API-04 → API-05 → API-06 → API-07
                │
                ▼
        TEST-01 → TEST-02 → TEST-03 → TEST-04 → TEST-05
        TEST-06 → TEST-07 → TEST-08 → TEST-09 → TEST-10
                │
                ▼
        DEVOPS-01 → DEVOPS-02 → DEVOPS-03 → DEVOPS-04
        DOC-01 → DOC-02
```

---

## Task Counting Summary

| Phase                                       | Tasks |
|---------------------------------------------|---|
| Phase 1 (Bug Fixes + Tech Foundation)       | 10 |
| Phase 2 (Value Objects)                     | 9 |
| Phase 3 (Aggregates + Domain Services)      | 7 |
| Phase 4 (Infrastructure + ACL + Resilience) | 10 |
| Phase 5(Testing)                            | 10 |

---

*End of PLAN.md — execution starts with FIX-01.*
