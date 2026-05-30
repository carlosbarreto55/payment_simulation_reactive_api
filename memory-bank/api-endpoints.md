# API Endpoints

## Authentication / User

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/user/register` | No | Register new user (email, password, name) |
| POST | `/api/user/login` | No | Authenticate, returns access + refresh tokens |
| POST | `/api/user/logout` | No | Revoke current refresh token |
| POST | `/api/user/refresh-token` | No | Rotate refresh token (returns new pair) |

### Auth Flows
- **Login**: Returns `accessToken` (5min), `refreshToken` (24h), `tokenType: Bearer`
- **Refresh**: Requires `refreshToken` in body. Returns new access + refresh token pair. Old refresh token is revoked.
- **Logout**: Requires `refreshToken` in body. Revokes it.

---

## Customer

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/customers` | JWT | Any | Create customer profile |
| GET | `/api/customers/{id}` | JWT | CUSTOMER/SUPPORT | Get customer by ID |
| PATCH | `/api/customers/{id}` | JWT | CUSTOMER/SUPPORT | Update customer fields |
| DELETE | `/api/customers/{id}` | JWT | CUSTOMER/SUPPORT | Delete customer |

### Access Rules
- CUSTOMER role: can only access own customer record
- SUPPORT role: can access any customer record

---

## Product

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/products` | JWT | Create product (name, description, price, currency, sku, stockQuantity) |
| GET | `/api/products/{id}` | JWT | Get product by ID |
| GET | `/api/products/sku/{sku}` | JWT | Get product by SKU |
| GET | `/api/products` | JWT | List active products |
| GET | `/api/products/available` | JWT | List products with stock > 0 |
| PUT | `/api/products/{id}` | JWT | Update product |
| DELETE | `/api/products/{id}` | JWT | Deactivate product (soft-delete) |
| POST | `/api/products/{id}/stock/decrease` | JWT | Decrease stock (validates availability) |
| POST | `/api/products/{id}/stock/increase` | JWT | Increase stock |

---

## Payment

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/payments/billing` | JWT | Create billing with PSP integration |

### Payment Request
- **Header**: `X-Idempotency-Key` (UUID, required)
- **Body**: `CreateBillingRequestDto` containing:
  - `customer` (name, email, document, type)
  - `products` (array of `{name, quantity, price}`)
  - `paymentMethod` (PIX/CARD)
  - `frequency` (ONE_TIME/MULTIPLE_PAYMENTS)

### Payment Response
- `externalPaymentId` — PSP transaction ID
- `status` — Current payment status
- `amount` — Calculated total
- `paymentMethod` — Used method
- `frequency` — Payment frequency
- `createdAt` — Timestamp

---

## Actuator (Monitoring)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/actuator/health` | No | Health check |
| GET | `/actuator/prometheus` | No | Prometheus metrics |
| GET | `/actuator/metrics` | No | Application metrics |
| GET | `/actuator/info` | No | Application info |

---

## Planned / Missing Endpoints
- GET `/api/payments/{id}` — Get payment intent by ID (missing)
- POST `/api/payments/webhook` — PSP webhook handler (missing)
- POST `/api/payments/{id}/refund` — Refund payment (missing)
- POST `/api/payments/{id}/confirm` — Confirm payment (missing)
