# Security Architecture

## Authentication Flow

### Registration
```
POST /api/user/register
  → Validate email uniqueness
  → BCrypt hash password
  → Create User (active=true)
  → Assign default role CUSTOMER
  → Return user info (no tokens)
```

### Login
```
POST /api/user/login
  → Find user by email
  → Verify BCrypt password
  → Generate access token (5min)
  → Generate refresh token (24h)
  → Persist refresh token
  → Return token pair
```

### Token Refresh
```
POST /api/user/refresh-token
  → Validate refresh token (exists, not expired, not revoked)
  → Revoke old refresh token (rotation)
  → Generate new access + refresh token pair
  → Persist new refresh token
  → Return new token pair
```

### Logout
```
POST /api/user/logout
  → Revoke provided refresh token
  → Cannot be used again
```

---

## JWT Token Details

### Access Token
- **Expiration**: 5 minutes (configurable via `jwt.access-token-expiration`)
- **Claims**: `sub` (userId), `email`, `roles` (role names), `iat`, `exp`
- **Usage**: Sent as `Authorization: Bearer <token>` header on every authenticated request

### Refresh Token
- **Expiration**: 24 hours (configurable via `jwt.refresh-token-expiration`)
- **Storage**: Persisted in `refresh_tokens` table (hashed or raw token, with `revoked` flag)
- **Usage**: Sent in body to `/api/user/refresh-token` endpoint

### Token Rotation
- Every refresh invalidates the old refresh token (sets `revoked = true`)
- Single-use refresh tokens — reuse of a rotated token is detected server-side

---

## Request Security Layer

### JwtAuthWebFilter (Security Filter)
- Intercepts all requests at `AUTHENTICATION` order
- Public paths bypass validation:
  - `/api/auth/register`
  - `/api/auth/login`
  - `/api/auth/refresh-token`
  - `/actuator/health`
- Extracts Bearer token from `Authorization` header
- Validates token via `JwtService.validateAccessToken()`
- Creates `JwtAuthToken` with `userId`, `email`, `roles`
- Sets `SecurityContext` for downstream usage

### SecurityConfig
- Disables CSRF (stateless API)
- Permits `/actuator/**` and `/api/user**` without authentication
- Adds `JwtAuthWebFilter` to the filter chain
- Provides `BCryptPasswordEncoder` bean

### @PreAuthorize Annotations
Used in CustomerController for fine-grained access:
- `hasRole('CUSTOMER')` or `hasRole('SUPPORT')` for customer CRUD
- Ownership validation in service layer (CUSTOMER can only access own record)

---

## RBAC Roles

| Role | Permissions |
|------|-------------|
| **CUSTOMER** | Can create/view/update/delete own customer profile. Default role on registration. |
| **MERCHANT_ADMIN** | Intended for merchant-level management (not yet fully wired). |
| **SUPPORT** | Can access any customer record, bypass ownership checks. |
| **SYSTEM** | Reserved for internal/system operations (not yet wired). |

---

## Security Configurations

### application.properties
```properties
jwt.secret=<HMAC-SHA key>
jwt.access-token-expiration=300000  # 5 minutes
jwt.refresh-token-expiration=86400000  # 24 hours
```

### Public Paths (inconsistency)
- `SecurityConfig` permits `/api/user**` (glob pattern covering all `/api/user/...` paths)
- `JwtAuthWebFilter` lists specific paths: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh-token`
- Note: The controller uses `/api/user/` prefix but the filter references `/api/auth/` — this mismatch works because `SecurityConfig` is more permissive

---

## Exception Classes (Security-Related)
| Exception | When Thrown |
|-----------|-------------|
| `InvalidCredentialsException` | Wrong email or password |
| `InvalidTokenException` | Malformed or expired JWT |
| `UnauthorizedException` | Insufficient permissions |
| `UserAlreadyExistsException` | Duplicate email on registration |
