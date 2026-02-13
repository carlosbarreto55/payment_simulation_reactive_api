# OpenAPI Documentation

## Overview

This directory contains the OpenAPI 3.0 specification for the Magalu Checkout & Payment API.

## Files

- **api-spec.yaml**: Complete OpenAPI 3.0 specification with all endpoints, schemas, and examples

## How to Use

### 1. View in Swagger UI Online

Visit [Swagger Editor](https://editor.swagger.io/) and paste the contents of `api-spec.yaml` to view the interactive documentation.

### 2. View in Swagger UI Locally (Docker)

```bash
docker run -p 8081:8080 \
  -e SWAGGER_JSON=/openapi/api-spec.yaml \
  -v $(pwd):/openapi \
  swaggerapi/swagger-ui
```

Then access: http://localhost:8081

### 3. View in Redoc (Docker)

```bash
docker run -p 8081:80 \
  -e SPEC_URL=/openapi/api-spec.yaml \
  -v $(pwd):/usr/share/nginx/html/openapi \
  redocly/redoc
```

Then access: http://localhost:8081

### 4. Import into Postman

1. Open Postman
2. Click **Import**
3. Select **File** → Choose `api-spec.yaml`
4. Postman will create a collection with all endpoints

### 5. Import into Insomnia

1. Open Insomnia
2. Click **Create** → **Import From** → **File**
3. Select `api-spec.yaml`
4. Insomnia will create a workspace with all requests

### 6. Generate Client Code

Use [OpenAPI Generator](https://openapi-generator.tech/) to generate client libraries:

```bash
# Java Client
openapi-generator-cli generate \
  -i api-spec.yaml \
  -g java \
  -o ./generated/java-client

# TypeScript/Axios Client
openapi-generator-cli generate \
  -i api-spec.yaml \
  -g typescript-axios \
  -o ./generated/typescript-client

# Python Client
openapi-generator-cli generate \
  -i api-spec.yaml \
  -g python \
  -o ./generated/python-client
```

## API Endpoints

### Authentication (Public)
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get tokens
- `POST /api/auth/logout` - Logout and revoke token
- `POST /api/auth/refresh-token` - Refresh access token

### Customers (Protected)
- `POST /api/customers` - Create customer
- `GET /api/customers/{id}` - Get customer by ID
- `PATCH /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

## Authentication Flow

1. **Register**: `POST /api/auth/register` with email and password
2. **Login**: `POST /api/auth/login` to get `accessToken` and `refreshToken`
3. **Use API**: Include `Authorization: Bearer <accessToken>` in headers
4. **Refresh**: When access token expires, use `POST /api/auth/refresh-token`
5. **Logout**: `POST /api/auth/logout` to revoke refresh token

## Security

### JWT Tokens

- **Access Token**: Expires in 5 minutes (300 seconds)
- **Refresh Token**: Expires in 24 hours (86400 seconds)

### RBAC (Role-Based Access Control)

#### Roles:
- **CUSTOMER**: End user, limited to own data
- **MERCHANT_ADMIN**: Store admin, extended access
- **SUPPORT**: Full access to all resources
- **SYSTEM**: Internal processes

#### Endpoint Permissions:

| Endpoint | CUSTOMER | MERCHANT_ADMIN | SUPPORT |
|----------|----------|----------------|---------|
| POST /customers | ✅ | ✅ | ✅ |
| GET /customers/{id} | ✅ (own) | ✅ (all) | ✅ (all) |
| PATCH /customers/{id} | ✅ (own) | ✅ (all) | ✅ (all) |
| DELETE /customers/{id} | ✅ (own) | ❌ | ✅ (all) |

## Validation Rules

### Email
- Must be valid email format
- Required for registration and customer creation

### Password
- Minimum 5 characters
- Maximum 255 characters

### Document Number
- 11-18 digits
- Only numeric characters
- Must be unique

### Phone Number
- Maximum 20 characters
- Format: digits, +, -, (), spaces

## Response Codes

| Code | Description |
|------|-------------|
| 200 | OK - Success (GET, PATCH) |
| 201 | Created - Resource created (POST) |
| 204 | No Content - Success without body (DELETE) |
| 400 | Bad Request - Validation failed |
| 401 | Unauthorized - Missing/invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Duplicate resource (email/document) |
| 500 | Internal Server Error |

## Examples

### Register and Login Flow

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123"
  }'

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123"
  }'

# Response:
# {
#   "accessToken": "eyJhbGci...",
#   "refreshToken": "eyJhbGci...",
#   "expiresIn": 3600,
#   "tokenType": "Bearer"
# }
```

### Create Customer

```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com",
    "documentType": "CPF",
    "documentNumber": "12345678901",
    "phoneNumber": "11987654321"
  }'
```

### Get Customer

```bash
curl -X GET http://localhost:8080/api/customers/1 \
  -H "Authorization: Bearer <accessToken>"
```

### Update Customer

```bash
curl -X PATCH http://localhost:8080/api/customers/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "name": "João Silva Santos",
    "email": "joao.santos@example.com",
    "phoneNumber": "11999999999"
  }'
```

### Delete Customer

```bash
curl -X DELETE http://localhost:8080/api/customers/1 \
  -H "Authorization: Bearer <accessToken>"
```

## Validation

The OpenAPI spec can be validated using:

```bash
# Using Swagger CLI
swagger-cli validate api-spec.yaml

# Using Spectral
spectral lint api-spec.yaml

# Using OpenAPI Generator
openapi-generator-cli validate -i api-spec.yaml
```

## Maintenance

When adding new endpoints or modifying existing ones:

1. Update `api-spec.yaml` with the changes
2. Add examples for request/response
3. Document all validation rules
4. Include appropriate error responses
5. Update this README if needed
6. Validate the spec before committing

## References

- [OpenAPI Specification 3.0](https://swagger.io/specification/)
- [Swagger Editor](https://editor.swagger.io/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Redoc](https://github.com/Redocly/redoc)
- [Spectral Linter](https://stoplight.io/open-source/spectral)
