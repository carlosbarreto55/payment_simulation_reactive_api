# Technology Stack

## Core Framework
- **Spring Boot 3.5.3** with Spring WebFlux (reactive stack)
- **Project Reactor** (Mono/Flux) for non-blocking execution

## Database & Data Layer
- **MySQL 8.0+** as primary database
- **R2DBC** (Reactive Relational Database Connectivity) via `io.asyncer:r2dbc-mysql`
- **Spring Data R2DBC** for reactive repository support
- **Flyway** for database migrations (11 migrations applied)

## Security
- **Spring Security WebFlux** for reactive security
- **JJWT 0.12.3** (`io.jsonwebtoken`) for JWT creation and validation
- **BCrypt** for password hashing
- Role-Based Access Control (RBAC) with 4 roles: CUSTOMER, MERCHANT_ADMIN, SUPPORT, SYSTEM

## HTTP Client
- **Spring WebClient** (reactive) for PSP integration
  - Connection pool: 100 max connections
  - Acquire timeout: 45s
  - Idle timeout: 20s
  - TLS enabled with compression

## Observability
- **Spring Boot Actuator** (health, metrics, prometheus endpoints)
- **Micrometer** with Prometheus registry

## API Documentation
- **OpenAPI 3.0** spec in `api-spec.yaml` (906 lines)

## Testing
- JUnit 5
- Reactor Test (StepVerifier)
- Testcontainers (MySQL)
- WebTestClient
- Spring Security Test

## DevOps
- Docker + Docker Compose (MySQL 8.0 + application)
- GitHub Actions (CI/CD pipeline)
- GitHub Container Registry (GHCR)

## Other
- **Lombok** for boilerplate reduction
- **Jakarta Validation** (Hibernate Validator) for DTO validation
- **Spring Boot DevTools** for development
