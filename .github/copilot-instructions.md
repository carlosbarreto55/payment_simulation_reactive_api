# Copilot Instructions

This document provides guidelines for AI-assisted development within the Magalu Checkout & Payment Platform project.

## Project Overview

**Project Name:** Payment Platform  
**Purpose:** A professional, production-grade reactive checkout and payment backend system, inspired by real-world architectures used by banks, fintechs, marketplaces, and major retailers.

This is a **Service-Oriented Architecture (SOA)** project specializing in payment processing and checkout flows, built with modern Java and reactive technologies. The system handles:

- Order creation and management
- Multi-method payment processing (PIX, Credit Card, Debit Card, Boleto)
- Risk analysis and fraud prevention
- Asynchronous event-driven communication
- JWT-based authentication and role-based authorization
- Idempotency in critical operations
- Audit logging and observability
- Webhook integrations

## Technology Stack

- **Language**: Java 17+
- **Framework**: Spring Boot 3.5.3 + Spring WebFlux
- **Persistence**: Spring Data R2DBC (Reactive)
- **Database**: MySQL 8.0+
- **Architecture**: Hexagonal Architecture with Domain-Driven Design
- **Testing**: JUnit 5, Testcontainers
- **Build Tool**: Maven

## Domain Architecture & Core Domains

The system is organized into independent, cohesive service domains, each with clear boundaries and well-defined responsibilities. Each domain follows complete layering: Entity → Repository → Service → Controller → DTO.

### 1. **Authentication & User Management Domain** (Auth Module)
**Status:** ~95% Complete  
**Responsibility:** User identity, authentication, authorization, and JWT token lifecycle management

**Core Entities:**
- `User`: Represents a user account (email, password_hash, enabled flag)
- `Role`: Represents user roles (CUSTOMER, MERCHANT_ADMIN, SUPPORT, SYSTEM)
- `UserRole`: Join table for many-to-many User-Role relationship
- `RefreshToken`: Stores refresh tokens with expiration and revocation tracking

**Key Services:**
- `JwtService`: Generates and validates JWT access and refresh tokens
  - Access tokens: 5-minute lifetime
  - Refresh tokens: 24-hour lifetime
  - Token validation with expiration and type checking
  
- `AuthenticationService`: Manages authentication flows
  - User registration with password hashing
  - Login with credential validation
  - Token generation and storage
  - Token refresh with old token revocation
  - Logout with token revocation

**API Endpoints:**
- `POST /api/auth/register`: Create new user account
- `POST /api/auth/login`: Authenticate and receive tokens
- `POST /api/auth/refresh-token`: Obtain new tokens using refresh token
- `POST /api/auth/logout`: Revoke refresh token

**Business Rules:**
- Emails must be unique
- Passwords are hashed using BCrypt before storage
- Access tokens expire after 5 minutes
- Refresh tokens expire after 24 hours
- Only non-revoked refresh tokens can be used to obtain new tokens
- Users must be enabled to successfully authenticate
- Refresh token rotation: old token is revoked when new tokens are generated

---

### 2. **Customer Domain**
**Status:** 70% Complete (Entity + Service done, Controller incomplete)  
**Responsibility:** Customer profiles and Know-Your-Customer (KYC) data management

**Core Entities:**
- `Customer`: Represents a customer entity with KYC information
  - Linked to User (1:1 relationship via user_id)
  - Stores customer name, document (CPF/CNPJ), phone
  - Tracks creation timestamp

**Key Services:**
- `CustomerService`: CRUD operations and customer lifecycle management

**Database:**
- Table: `customers` with FK to `users` table
- Unique constraint on document field
- Indexes on user_id and document for fast lookups

**Business Rules:**
- One User can have exactly one Customer profile
- Customer data is immutable after creation (in current implementation)
- Document values must be unique across all customers
- Customer creation must be associated with an authenticated user

---

### 3. **Order Management Domain**
**Status:** 0% (Not yet implemented)  
**Responsibility:** Shopping cart checkout, order creation, state management, and order lifecycle

**Planned Entities:**
- `Order`: Main order entity
  - States: CREATED → RESERVED → PAID | CANCELED | REFUNDED
  - Links to Customer (many orders per customer)
  - Tracks total amount, timestamps
  
- `OrderItem`: Individual items in an order
  - Links to Order
  - Stores product_id, product_name, quantity, unit_price, total_price
  - Supports decimal prices for accurate financial calculations

**Planned Business Rules:**
- Order can only transition from CREATED → RESERVED
- RESERVED state indicates stock has been reserved
- Order can only be paid if in CREATED or RESERVED state
- PAID orders cannot be directly canceled (must be refunded instead)
- Refunds create a separate transaction preserving original order state
- Order amounts are immutable after creation
- Orders support partial refunds (future enhancement)
- Inventory reservation failure prevents order progression

---

### 4. **Payment Processing Domain**
**Status:** 0% (Not yet implemented)  
**Responsibility:** Payment intent creation, transaction processing, and payment state management

**Planned Entities:**
- `PaymentIntent`: Captures payment request intent
  - States: PENDING → PROCESSING → APPROVED | DENIED | REFUNDED
  - Stores order_id, amount, currency (BRL), payment method
  - Includes idempotency_key for duplicate prevention
  - Links to RiskAnalysis via one-to-one relationship
  
- `PaymentTransaction`: Records actual payment attempts
  - Links to PaymentIntent
  - Stores external_id (from payment gateway)
  - Tracks status, failure_reason, processed_at timestamp

**Supported Payment Methods:**
- PIX (instant transfer)
- CREDIT_CARD (simulated processing)
- DEBIT_CARD (simulated processing)
- BOLETO (simulated processing)

**Planned Business Rules:**
- Idempotency key prevents duplicate charges
- One PaymentIntent per Order
- Payment processing is synchronous (via simulated gateway)
- Transaction records are immutable after creation
- Failed payments generate audit logs
- Only APPROVED payments mark order as PAID
- DENIED payments do not change order state

---

### 5. **Risk Analysis & Fraud Prevention Domain**
**Status:** 0% (Not yet implemented)  
**Responsibility:** Fraud detection, risk scoring, and approval workflow

**Planned Entities:**
- `RiskAnalysis`: Fraud analysis result per payment intent
  - One-to-one relationship with PaymentIntent
  - Stores risk score (0-100)
  - Decision states: APPROVE, REVIEW, DENY
  - Tracks reviewer (User) and review timestamp
  - Stores reason for decision

**Planned Business Rules:**
- Risk analysis happens synchronously before payment processing
- Automatic scoring based on payment characteristics
- REVIEW decisions require manual approval from support staff
- DENY decisions block the payment immediately
- Risk score can be updated after initial analysis (workflow)
- Only SUPPORT or MERCHANT_ADMIN roles can manually review

---

### 6. **Event & Notification Domain** (Future)
**Status:** 0% (Not yet implemented)  
**Responsibility:** Asynchronous event handling and external notifications

**Planned Entities:**
- `OutboxEvent`: Implements Outbox Pattern for event reliability
  - Stores: aggregate_type, aggregate_id, event_type, payload
  - States: PENDING → PROCESSED | FAILED
  - Includes retry_count for resilience
  - Timestamps for audit trail

**Planned Integrations:**
- Webhook notifications for order state changes
- Email notifications for transactions
- Event consumption for downstream services
- Guaranteed at-least-once event delivery

**Business Rules:**
- Events are persisted transactionally with domain changes
- Failed events retry with exponential backoff
- Event ordering is preserved per aggregate
- Webhook signatures use HMAC for security

---

### 7. **Idempotency Key Management Domain**
**Status:** Infrastructure ready (migrations created)  
**Responsibility:** Duplicate request prevention for critical operations

**Planned Entities:**
- `IdempotencyKey`: Stores request/response pairs for safe retries
  - Stores: key_value, request_method, request_path
  - Cached response: response_status, response_body
  - Expiration strategy for cleanup
  - TTL-based auto-cleanup

**Business Rules:**
- Idempotency keys are case-sensitive, unique
- Same key always returns same response
- Keys expire after defined TTL (default: 24 hours)
- Used primarily for payment operations and order creation

---

### 8. **Audit & Compliance Domain**
**Status:** Infrastructure ready (migrations created)  
**Responsibility:** Compliance audit trails and activity logging

**Planned Entities:**
- `AuditLog`: Records all significant system events
  - Stores: user_id, action, resource_type, resource_id, details
  - Includes IP address for security tracking
  - Immutable after creation

**Business Rules:**
- Every payment, order change, and user action is logged
- Audit logs are immutable and retained indefinitely
- Used for compliance reporting and forensic analysis
- Includes both technical and business events

---

## Cross-Domain Business Rules & Constraints

### Authentication & Authorization
- All authenticated endpoints require valid JWT in `Authorization: Bearer <token>` header
- Users must be in enabled status to authenticate
- Role-based access control enforced:
  - **CUSTOMER**: Can view/manage own data only (ownership validation)
  - **MERCHANT_ADMIN**: Extended permissions for store operations
  - **SUPPORT**: Full access for support functions
  - **SYSTEM**: Internal system processes (service-to-service)
- Access tokens are 5-minute lived, refresh tokens are 24-hour lived
- Token refresh automatically revokes old refresh token (token rotation)

### Payment Processing Flow
1. **Order Creation**: Customer creates order with items
2. **Stock Reservation**: System reserves inventory (simulated)
3. **Payment Intent Creation**: Payment request created with idempotency key
4. **Risk Analysis**: Automated fraud analysis performed
   - If DENY: Payment rejected immediately
   - If REVIEW: Payment waits for manual approval
   - If APPROVE: Payment proceeds
5. **Payment Processing**: Payment gateway processes (simulated)
   - Gateway returns status (APPROVED/DENIED)
6. **Order State Update**: Order marked as PAID only if payment APPROVED
7. **Event Publishing**: System events published to Outbox

### Idempotency Guarantees
- All critical operations (payments, orders) use idempotency keys
- Duplicate requests with same key return cached response
- Prevents duplicate charges and race conditions
- Keys stored with TTL (24 hours default)

### Data Integrity Constraints
- Currency is fixed to BRL (Brazilian Real)
- Amounts are stored as DECIMAL(19,2) for financial accuracy
- No negative amounts allowed on any transaction
- Order totals calculated from order items (sum validation)
- Foreign keys enforce referential integrity:
  - Customer ← User (1:1 via FK)
  - Order ← Customer (N:1)
  - OrderItem ← Order (N:1, CASCADE delete)
  - PaymentIntent ← Order (1:1)
  - PaymentTransaction ← PaymentIntent (N:1)
  - RiskAnalysis ← PaymentIntent (1:1)

### State Machine Constraints

**Order State Flow:**
```
CREATED ──→ RESERVED ──→ PAID
  ↓          ↓          (final)
CANCELED   CANCELED
  (final)    (final)

REFUNDED (from PAID only, final)
```

**PaymentIntent State Flow:**
```
PENDING ──→ PROCESSING ──→ APPROVED (final)
             ↓
             DENIED (final)
             
REFUNDED (from APPROVED only, final)
```

**RiskAnalysis Decision:**
- APPROVE: Payment can proceed
- REVIEW: Requires manual approval from SUPPORT/MERCHANT_ADMIN
- DENY: Payment rejected, order not modified

### Security & Data Privacy
- Passwords hashed with BCrypt (never stored in plain text)
- Sensitive data (tokens, keys) not logged
- IP addresses tracked in audit logs for security
- HTTPS enforced in production
- HMAC signatures for webhook authentication
- SQL injection prevention via parameterized queries (R2DBC)

### Rate Limiting & Resilience
- Token expiration enforces session limits
- Revocation checks prevent use of compromised tokens
- Retry logic for failed payment attempts (future enhancement)
- Circuit breaker pattern for external integrations (future)
- Exponential backoff for event processing failures

## Code Generation Guidelines

### Reactive Programming

- Use reactive patterns only for I/O-bound operations and high-concurrency scenarios
- Avoid blocking calls in reactive pipelines (no `.block()` or `.subscribe()`)
- Prefer composition operators: `flatMap`, `map`, `zip`, `filter`, `reduce`
- Keep reactive chains readable and maintainable
- Use meaningful variable names for operator pipelines

### Architecture & Design

Follow these architectural principles:

- **Hexagonal Architecture**: Clear separation between domain, application, and infrastructure layers
- **Domain-Driven Design**: Rich domain models with expressive business logic
- **SOLID Principles**: Single responsibility, open/closed, Liskov substitution, interface segregation, dependency inversion
- **Immutability**: Prefer immutable objects and final fields where applicable
- **Clean Code**: Prioritize readability and maintainability over cleverness

### Layering

```
├── domain/           # Pure business logic, independent of frameworks
├── application/      # Use cases, orchestration, DTOs
├── infrastructure/   # Database, external integrations
└── interfaces/       # REST controllers, message handlers
```

Domain layer must remain framework-agnostic.

### Database & Persistence

- Use Spring Data R2DBC for reactive database access
- Avoid N+1 query problems through proper entity design
- Implement proper indexing strategies
- Use database migrations (Flyway/Liquibase)
- Define transaction boundaries clearly
- Support pagination for large datasets
- Normalize schema appropriately

### API Design

- Follow RESTful principles
- Use consistent naming conventions
- Document endpoints in OpenAPI/Swagger format
- Implement proper HTTP status codes
- Use DTOs for request/response contracts
- Include idempotency keys for critical operations

### Security & Authentication

- Implement JWT-based authentication with refresh token rotation
- Use Spring Security with reactive components
- Apply role-based access control (RBAC)
- Validate all inputs
- Use HTTPS in production
- Secure token storage and transmission
- Protect against common vulnerabilities (CSRF, XSS, SQL injection)

### Error Handling

- Create domain-specific exceptions extending a base exception
- Implement global exception handlers
- Return meaningful error responses with proper HTTP status codes
- Log errors appropriately without exposing sensitive information
- Include error tracking and correlation IDs

### Testing

- Write unit tests for domain logic
- Use integration tests for infrastructure components
- Use Testcontainers for database testing
- Mock only external dependencies, not domain logic
- Write behavior-oriented tests
- Ensure tests are deterministic and readable
- Use StepVerifier for reactive testing

### Observability

- Implement structured logging
- Use correlation IDs for distributed tracing
- Log business and technical events meaningfully
- Use appropriate log levels (DEBUG, INFO, WARN, ERROR)
- Include metrics collection
- Enable centralized logging in production

### Microservices Patterns

- Design for loose coupling
- Ensure idempotency for critical operations
- Implement retry and circuit breaker patterns
- Use event-driven communication when beneficial
- Consider failure scenarios in design
- Design clear API contracts

## Code Review Checklist

Before committing code, verify:

- [ ] No blocking operations in reactive code
- [ ] Proper exception handling
- [ ] Tests are included and passing
- [ ] Code follows project conventions
- [ ] No sensitive data in logs
- [ ] Database migrations are versioned
- [ ] Documentation is updated
- [ ] Performance implications considered
- [ ] Security best practices followed
- [ ] Code is readable and maintainable

## File Organization

```
src/main/java/checkout/
├── common/              # Shared utilities, constants, exceptions
├── config/              # Application configuration
├── domain/              # Domain models and business logic
│   ├── customer/
│   ├── payment/
│   └── user/
├── application/         # Application services (use cases)
├── infrastructure/      # Repository implementations, adapters
└── interfaces/          # REST controllers, event handlers
```

Domain layer must remain framework-agnostic.

## Current Implementation Status

### ✅ Completed Components

#### Exception System (21 exception classes)
- **Base:** `BaseException`, `BusinessRuleException`
- **Auth:** `InvalidCredentialsException`, `InvalidTokenException`, `UnauthorizedException`, `UserAlreadyExistsException`
- **Payment:** `PaymentAlreadyProcessedException`, `PaymentMethodNotSupportedException`, `PaymentNotFoundException`, `InvalidPaymentStatusException`
- **Order:** `OrderNotFoundException`, `OrderAlreadyPaidException`, `OrderCancellationNotAllowedException`, `InvalidOrderStatusException`
- **Risk:** `RiskAnalysisDeniedException`
- **Stock:** `StockReservationFailedException`
- **Idempotency:** `DuplicateIdempotencyKeyException`
- **Generic:** `ResourceNotFoundException`
- **Response:** `ErrorResponse` DTO with proper HTTP mapping
- **Handler:** `GlobalExceptionHandler` with reactive error handling

#### Authentication Module (95% Complete)
- **Entities:** User, Role, UserRole, RefreshToken (all with proper R2DBC annotations)
- **Repositories:** UserRepository, RoleRepository, UserRoleRepository, RefreshTokenRepository
- **Services:**
  - `JwtService`: Token generation/validation (access & refresh)
  - `AuthenticationService`: Register, Login, Token Refresh, Logout flows
  - `UserService`: User CRUD operations
- **Controllers:** `UserController` with 4 endpoints (register, login, refresh, logout)
- **DTOs:** RegisterRequestDto, RegisterResponseDto, LoginRequestDto, LoginResponseDto, RefreshTokenRequestDto
- **Filters:** `JwtAuthWebFilter` for request interception
- **Configuration:** `JwtAuthWebFilter`, `JwtAuthToken`, `SecurityConfig`

#### Customer Module (70% Complete)
- **Entities:** Customer with proper relationships
- **Repositories:** CustomerRepository
- **Services:** CustomerService (CRUD operations)
- **DTOs:** Ready for implementation
- **Status:** Controller layer 40% complete

#### Infrastructure
- **Database Migrations:** 9 Flyway migrations (V1-V9)
  - V1: Users, Roles, UserRoles, RefreshTokens
  - V2: Customers
  - V3: Orders, OrderItems
  - V4: PaymentIntents, PaymentTransactions
  - V5: RiskAnalyses
  - V6: IdempotencyKeys
  - V7: OutboxEvents
  - V8: AuditLogs
  - V9: Role initialization
- **R2DBC Configuration:** Fully configured with connection pooling
- **Security Configuration:** JWT filter and security rules

### ❌ Not Yet Implemented

#### Order Module (0%)
- Entity and Controller layers
- Service layer for state management
- Order creation and modification flows

#### Payment Module (0%)
- PaymentIntent and PaymentTransaction services
- Payment gateway integration (simulated)
- Transaction processing logic

#### Risk Analysis Module (0%)
- RiskAnalysis service and controller
- Fraud scoring algorithm
- Manual review workflow

#### Notification/Event Module (0%)
- OutboxEvent service
- Event publishing and consumption
- Webhook integration

#### Idempotency Service (0%)
- IdempotencyKey service
- Cache-aside pattern for response memoization

#### Audit Service (0%)
- AuditLog service
- Activity logging aspect or decorator

## Configuration Files

### application.properties
- JWT secret key and expiration times configured
- R2DBC MySQL connection properties
- Logging configuration (SLF4J)
- Port: 8080 (default)

### Docker & DevOps
- `Dockerfile`: Multi-stage build for optimized images
- `docker-compose.yml`: MySQL 8.0 + Spring Boot application stack
- `mvnw`/`mvnw.cmd`: Maven wrapper for build consistency

## API Documentation

**OpenAPI Specification:** `src/main/resources/openapi/api-spec.yaml`
- Complete endpoint documentation
- Request/response schemas
- Security schemes (Bearer JWT)
- Example requests and responses for all endpoints

**Implemented Endpoints:**
- `POST /api/auth/register` - Create new user
- `POST /api/auth/login` - Authenticate user
- `POST /api/auth/refresh-token` - Refresh tokens
- `POST /api/auth/logout` - Revoke refresh token

**Planned Endpoints:**
- Customers: Create, Read, Update, Delete
- Orders: Create, Read, Cancel, List
- Payments: Create, Process, Refund, List
- Risk Analysis: Create, Review, Approve/Deny
- Webhooks: Receive, Retry, List events

## Configuration

- Externalize configuration using `application.properties`
- Use environment variables for sensitive data
- Implement profiles for different environments (dev, staging, prod)

## Documentation

- Keep README files updated
- Document complex business logic
- Maintain OpenAPI specifications
- Include architecture decision records (ADRs) when relevant

## When to Ask for Help

If you encounter:

- Ambiguity about architectural decisions
- Performance optimization needs
- Complex reactive patterns
- Security concerns
- Distributed system trade-offs

Provide context about the specific challenge, and reference this guide for consistency.

## CI/CD & DevOps

- Use Docker for containerization
- Implement automated testing in CI/CD pipelines
- Follow infrastructure-as-code principles when applicable
- Secure secrets management
- Support multi-environment deployments

---

**Last Updated**: February 2026
