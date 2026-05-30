# Reactive Checkout and Payment Platform

Professional checkout and payment backend developed with **Java** and **Spring Boot**, using reactive programming with **Spring WebFlux** and **MySQL (R2DBC)**. This project simulates a real-world checkout system, inspired by architectures used by large companies such as banks, fintechs, marketplaces, and retailers.

## Objective

Build a reactive backend that represents a **complete checkout and payment system**, covering:

- Order creation and management
- Payment processing (PIX, credit card, debit card, boleto - simulated)
- Risk analysis and fraud prevention
- Webhooks and asynchronous events
- Robust authentication and authorization (JWT)
- CI/CD pipeline with GitHub Actions
- Idempotency in critical operations
- Audit and observability

## Architecture

This project follows a **Service-Oriented Architecture (SOA)** pattern, organizing the application into independent, cohesive services that communicate through well-defined interfaces and asynchronous events.

```
┌──────────────────────────────────────────────────────────────┐
│                         API Layer                            │
│                   (Spring WebFlux REST)                      │
└───────┬──────────┬──────────┬──────────┬──────────┬─────────┘
        │          │          │          │          │
   ┌────▼───┐ ┌───▼────┐ ┌───▼─────┐ ┌──▼────┐ ┌──▼────────┐
   │  Auth  │ │ Order  │ │ Payment │ │ Risk  │ │ Webhook   │
   │Service │ │Service │ │ Service │ │Service│ │  Service  │
   └────┬───┘ └───┬────┘ └───┬─────┘ └──┬────┘ └──┬────────┘
        │         │          │          │         │
        └─────────┴──────────┴──────────┴─────────┘
                           │
                  ┌────────▼────────┐
                  │  Event Bus      │
                  │ (Outbox Pattern)│
                  └────────┬────────┘
                           │
                  ┌────────▼────────┐
                  │ MySQL (R2DBC)   │
                  └─────────────────┘
```

### Service-Oriented Architecture Characteristics

**Independent Services:**
Each domain service (Auth, Order, Payment, Risk, Webhook, Notification) is independently developed with its own:
- Domain entities and business logic
- Repository layer for data access
- Service layer for orchestration
- Controller layer exposing RESTful APIs
- DTOs for input/output contracts

**Service Communication:**
- **Synchronous:** RESTful APIs for direct service-to-service calls
- **Asynchronous:** Event-driven communication via Outbox Pattern for eventual consistency
- **Decoupled:** Services interact through well-defined contracts, enabling independent evolution

**Key Architectural Patterns:**
- **Reactive Programming:** Non-blocking operations using Project Reactor
- **Outbox Pattern:** Guaranteed event delivery for asynchronous communication
- **Idempotency:** Duplicate request prevention in critical operations
- **RBAC:** Role-Based Access Control for authorization
- **Domain Separation:** Clear boundaries between business contexts
- **Layered Architecture:** Separation of concerns (API, Domain, Infrastructure)

## Technology Stack

### Backend
- **Java 17+**
- **Spring Boot 3.5.3**
- **Spring WebFlux** (Reactive Programming)
- **Spring Security** (JWT Authentication)
- **Spring Data R2DBC** (Reactive MySQL)
- **Flyway** (Database migrations)
- **Lombok** (Boilerplate reduction)

### Database
- **MySQL 8.0+**
- **R2DBC MySQL Driver** (Reactive driver)

### Security
- **JWT** (JSON Web Tokens) - Access + Refresh Token
- **Spring Security WebFlux**
- **HMAC** (Webhook signatures)

### Observability
- **Spring Boot Actuator**
- **Micrometer** + **Prometheus**
- **Structured logging**

### Testing
- **JUnit 5**
- **Reactor Test**
- **Testcontainers** (MySQL for integration tests)
- **WebTestClient**

### DevOps
- **Docker** + **Docker Compose**
- **GitHub Actions** (CI/CD)
- **GitHub Container Registry** (GHCR)

## System Services

### Identity & Auth Service
- User registration and login
- JWT with access and refresh tokens
- Access profiles:
  - `CUSTOMER` - End customer
  - `MERCHANT_ADMIN` - Store administrator
  - `SUPPORT` - Technical support
  - `SYSTEM` - Internal system

### Customer & Merchant Service
- Customer management (simplified KYC)
- Merchant and store management
- Relationship: customer → order → payment

### Order Service
- Order creation
- Order states:
  - `CREATED` - Created
  - `RESERVED` - Stock reserved
  - `PAID` - Paid
  - `CANCELED` - Canceled
  - `REFUNDED` - Refunded
- Stock reservation (simulated)
- Safe cancellation

### Payment Service (Core)
- **Payment Intent** creation
- Payment methods:
  - **PIX**
  - **Credit Card** (simulated)
  - **Debit Card** (simulated)
  - **Boleto** (simulated)
- **Idempotency-Key** mandatory
- Duplicate charge prevention
- Refund support

### Risk Analysis Service
- Simulated risk rules
- Decisions:
  - `APPROVE` - Approved
  - `REVIEW` - Under review
  - `DENY` - Denied
- Manual approval via backoffice (SUPPORT profile)

### Notification Service
- Business events:
  - Order paid
  - Payment declined
  - Refund processed
- Delivery via:
  - Email (mock)
  - Merchant webhook
- Implementation with **Outbox Pattern**

### Webhook Service
- Webhook configuration per store
- HMAC signature for security
- Webhook idempotency
- Automatic retry with exponential backoff

## Main Business Rules

### Authentication & Access
- A user can only access orders they created, except:
  - `SUPPORT` → can only **view** any order
  - `MERCHANT_ADMIN` → can view orders from their own store
- Invalid refresh token automatically invalidates all active user tokens
- Excessive login attempts temporarily block the user

### Orders
- An order can only be paid if in `CREATED` or `RESERVED` state
- A `PAID` order **cannot** be canceled directly - only refunded
- If stock reservation fails, the order is automatically canceled
- An order can only have **one active payment at a time**

### Payments
- Every payment creation/confirmation request **requires Idempotency-Key**
- Idempotent requests always return the **same response**, without duplicating transactions
- An approved payment:
  - Updates the order to `PAID`
  - Generates notification event
- A denied payment:
  - Keeps the order in `CREATED`
  - Records technical and business reason
- Refunds are only allowed for `APPROVED` payments

### Fraud Prevention
- Payments above a threshold automatically enter `REVIEW`
- Multiple payment attempts in a short period increase risk score
- `DENY` payments:
  - Cannot be reprocessed
  - Require creation of a new order
- Only `SUPPORT` users can manually approve a `REVIEW`

### Webhooks & Events
- Webhooks must contain valid HMAC signature
- Webhooks are **idempotent** (safe reprocessing)
- Delivery failures do not lose events (Outbox Pattern)
- Events are delivered **at least once**

## How to Run

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (optional, but recommended)
- MySQL 8.0+ (if not using Docker)

### Option 1: With Docker Compose (Recommended)

1. Clone the repository:
```bash
git clone <repository-url>
cd magalu
```

2. Run Docker Compose:
```bash
docker-compose up -d
```

This will start:
- MySQL on port 3306
- Spring Boot application on port 8080

3. Access the application:
```
http://localhost:8080
```

### Option 2: Local Execution

1. Configure MySQL:
```sql
CREATE DATABASE magalu_db;
CREATE USER 'root'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON magalu_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

2. Configure `application.properties` with your credentials:
```properties
spring.r2dbc.url=r2dbc:mysql://localhost:3306/magalu_db
spring.r2dbc.username=root
spring.r2dbc.password=your_password
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

### Check Application Health

```bash
curl http://localhost:8080/actuator/health
```

## Project Structure

```
magalu/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── checkout/
│   │   │       ├── config/              # Configurations (Security, R2DBC)
│   │   │       ├── domain/
│   │   │       │   ├── auth/           # Identity & Auth Service
│   │   │       │   ├── customer/       # Customer & Merchant Service
│   │   │       │   ├── order/          # Order Service
│   │   │       │   ├── payment/        # Payment Service (Core)
│   │   │       │   ├── risk/           # Risk Analysis Service
│   │   │       │   ├── notification/   # Notification Service
│   │   │       │   └── webhook/        # Webhook Service
│   │   │       ├── boundedcontext/     # New DDD value objects & enums
│   │   │       │   ├── customer/domain/# Customer VOs (Document, etc.)
│   │   │       │   └── payment/domain/ # Payment VOs (PaymentMethod, IdempotencyKey, etc.)
│   │   │       ├── common/
│   │   │       │   ├── exception/      # Custom exceptions
│   │   │       │   ├── security/       # Security utilities
│   │   │       │   ├── idempotency/    # Idempotency Key
│   │   │       │   └── audit/          # Audit
│   │   │       └── messaging/          # Asynchronous events
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/           # Flyway migrations
│   └── test/
│       └── java/                       # Tests
├── pom.xml
├── docker-compose.yml
├── Dockerfile
└── README.md
```

> **DDD Package Layout Note:** The project uses a transitional package layout. Legacy controllers, services, repositories, entities, and DTOs live under `checkout.domain.*`, while new DDD value objects and domain enums are placed under `checkout.boundedcontext.*` during the incremental migration.

## Main Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login
- `POST /api/auth/refresh` - Renew access token
- `POST /api/auth/logout` - Logout

### Orders
- `POST /api/orders` - Create order
- `GET /api/orders/{id}` - Get order
- `POST /api/orders/{id}/cancel` - Cancel order

### Payments
- `POST /api/payments/intents` - Create payment intent
- `POST /api/payments/{id}/confirm` - Confirm payment
- `POST /api/payments/{id}/refund` - Refund payment

### Fraud Prevention
- `POST /api/risk/analyze` - Analyze risk
- `POST /api/risk/{id}/approve` - Manually approve (SUPPORT)

### Webhooks
- `POST /api/webhooks/config` - Configure webhook
- `GET /api/webhooks/deliveries` - List deliveries

## Testing

### Run Tests
```bash
./mvnw test
```

### Run Tests with Coverage
```bash
./mvnw test jacoco:report
```

### Integration Tests
Integration tests use **Testcontainers** to create an isolated MySQL environment.

## CI/CD

The project has a CI/CD pipeline configured with **GitHub Actions**:

1. **Pull Request**: Executes build and tests
2. **Build**: Generates Docker image and pushes to GHCR
3. **Deploy**: Automatic deploy to staging, manual to production

## Observability

### Prometheus Metrics
```
http://localhost:8080/actuator/prometheus
```

### Health Check
```
http://localhost:8080/actuator/health
```

### Metrics
```
http://localhost:8080/actuator/metrics
```

## Security

- **JWT** with refresh token
- **Spring Security WebFlux**
- **Rate limiting** on sensitive endpoints
- **Sensitive data masking** in logs
- **HMAC signature** on webhooks
- **Input validation** on all endpoints

## Applied Patterns

- **Reactive Programming** (Reactor)
- **Outbox Pattern** (Asynchronous events)
- **Idempotency** (Critical operations)
- **Transactional Operator** (R2DBC)
- **RBAC** (Role-Based Access Control)
- **Layer Separation** (API, domain, infrastructure)
- **Domain-Driven Design** (DDD)
- **Service-Oriented Architecture** (SOA)

## Contributing

1. Fork the project
2. Create a branch for your feature (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is under the MIT license. See the `LICENSE` file for more details.

## Author

Developed as a professional portfolio project, demonstrating knowledge in:
- Reactive systems architecture
- Spring WebFlux and reactive programming
- Design and architecture patterns
- Development best practices
- CI/CD and DevOps
- Service-Oriented Architecture

## Useful Links

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [R2DBC Documentation](https://r2dbc.io/)
- [Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

If this project was useful to you, consider giving it a star!
