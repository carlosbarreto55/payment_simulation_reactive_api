# 🏗️ Arquitetura - Payment Provider Integration

## Fluxo Completo de Pagamento

```
┌─────────────────┐
│   Cliente Web   │
│    ou Mobile    │
└────────┬────────┘
         │ (1) Criar Pedido
         ▼
┌──────────────────────────────────────────────────────────────┐
│               APLICAÇÃO MAGALU CHECKOUT                      │
│  ┌────────────────────────────────────────────────────────┐  │
│  │          PaymentController                             │  │
│  │   POST /api/payments/process (idempotency-key)         │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │ (2) Inicia processamento                │
│  ┌────────────────▼─────────────────────────────────────┐  │
│  │     BillingProcessingService                         │  │
│  │   - Validar BillingIntent (status PENDING)           │  │
│  │   - Verificar idempotência                           │  │
│  │   - Chamar PaymentProviderClient                      │  │
│  │   - Persistir BillingTransaction                      │  │
│  │   - Atualizar Order status → PAID                     │  │
│  │   - Publicar evento                                   │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │ (3) Chamar API externa                  │
│  ┌────────────────▼─────────────────────────────────────┐  │
│  │     PaymentProviderClient (WebClient)                │  │
│  │   - Circuit Breaker                                  │  │
│  │   - Retry (exponential backoff)                       │  │
│  │   - Timeout management                               │  │
│  │   - Correlation ID logging                           │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                         │
│              (4) HTTP POST                                  │
│              CreateBillingRequestDto                        │
│                   │                                         │
└───────────────────┼─────────────────────────────────────────┘
                    │
                    ▼
        ╔═══════════════════════════════════╗
        ║   PAYMENT PROVIDER (Gateway)      ║
        ║   https://api.abacatepay.com      ║
        ║   /v1/billing/create              ║
        ║                                   ║
        ║   (5) Processa pagamento          ║
        ║       PIX | CARD | BOLETO         ║
        ╚═══════════╦═══════════════════════╝
                    │
                    │ (6) Response
                    │ CreateBillingResponseDto
                    │ (status, url, qrCode, etc)
                    │
┌───────────────────▼─────────────────────────────────────────┐
│               APLICAÇÃO MAGALU CHECKOUT                      │
│  ┌────────────────────────────────────────────────────────┐  │
│  │     BillingProcessingService (cont.)                  │  │
│  │   - Parse response                                    │  │
│  │   - Update BillingIntent.externalBillingId            │  │
│  │   - Create BillingTransaction                         │  │
│  │   - Update Order.status = PAID                        │  │
│  │   - Publish OrderPaid event                           │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │ (7) Retorna resultado                   │
│  ┌────────────────▼─────────────────────────────────────┐  │
│  │        PaymentController                             │  │
│  │   Response 200 + BillingProcessingResult              │  │
│  └─────────────────────────────────────────────────────┘  │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │   Cliente recebe     │
        │   Payment URL        │
        │   (para redireção)   │
        └──────────────────────┘
```

---

## Estrutura de Camadas (Hexagonal Architecture)

```
┌─────────────────────────────────────────────────────────────────┐
│                    INTERFACES (REST)                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PaymentController                                       │   │
│  │  POST /api/payments/process → BillingProcessingResult   │   │
│  │  GET  /api/payments/{id}    → BillingIntentResponseDto  │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────┬─────────────────────────────────────────────────────┘
             │ HTTP
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  BillingProcessingService (Use Case)                    │   │
│  │  - Orchestrate payment flow                             │   │
│  │  - Validate idempotency                                 │   │
│  │  - Call external APIs                                   │   │
│  │  - Update domain state                                  │   │
│  │  - Publish events                                       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  BillingIntentService (CRUD)                            │   │
│  │  - Create, Read, Update BillingIntent                   │   │
│  │  - Validate business rules                              │   │
│  │  - Query by idempotency key                             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  BillingRequestMapper                                   │   │
│  │  Order → CreateBillingRequestDto                        │   │
│  │  Prepare data for Payment Provider                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────┬─────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Domain Models                                          │   │
│  │  - BillingIntent (entity)                               │   │
│  │  - BillingTransaction (entity)                          │   │
│  │  - BillingStatus (enum)                                 │   │
│  │  - PaymentMethod (enum)                                 │   │
│  │  - Business rules & validations                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  DTOs (Domain Transfer Objects)                         │   │
│  │  - CreateBillingRequestDto (request)                    │   │
│  │  - CreateBillingResponseDto (response)                  │   │
│  │  - BillingIntentResponseDto (internal API)              │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────┬─────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PaymentProviderClient (HTTP Client)                    │   │
│  │  - WebClient (reactive)                                 │   │
│  │  - Circuit Breaker (Resilience4j)                       │   │
│  │  - Retry strategy (exponential backoff)                 │   │
│  │  - Error handling & mapping                             │   │
│  │  - Correlation ID propagation                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Repositories (R2DBC)                                   │   │
│  │  - BillingIntentRepository                              │   │
│  │  - BillingTransactionRepository                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  PaymentProviderConfig                                  │   │
│  │  - WebClient bean                                       │   │
│  │  - Circuit Breaker registry                             │   │
│  │  - Retry registry                                       │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────┬─────────────────────────────────────────────────────┘
             │
             ▼
        ┌────────────────────────┐
        │   MySQL Database       │
        │  - billing_intents     │
        │  - billing_transactions│
        └────────────────────────┘
```

---

## Fluxo de Dados - Request

```
Cliente HTTP
    │
    ├─ Headers:
    │   ├── Authorization: Bearer <jwt-token>
    │   ├── Idempotency-Key: <uuid>
    │   └── Content-Type: application/json
    │
    ├─ Body:
    │   └── CreateBillingIntentRequestDto
    │       ├── orderId: Long
    │       ├── customerId: Long
    │       ├── amount: Long (centavos)
    │       ├── methods: ["PIX", "CARD"]
    │       └── returnUrl: String
    │
    ▼
PaymentController::processBilling()
    │
    ├─ Autorização (JWT)
    ├─ Validação de Idempotency-Key (header obrigatório)
    ├─ Validação de payload
    │
    ▼
BillingProcessingService::processBilling()
    │
    ├─ Validar BillingIntent (status PENDING)
    ├─ Verificar duplicata (idempotencyKey)
    ├─ Mapear → CreateBillingRequestDto
    │
    ▼
PaymentProviderClient::createBilling()
    │
    ├─ @CircuitBreaker(fallback)
    ├─ @Retry(exponentialBackoff)
    ├─ WebClient POST /v1/billing/create
    │   └─ Payload: CreateBillingRequestDto
    │
    ▼
PAYMENT PROVIDER API
    │
    ├─ Processa pagamento
    ├─ Retorna CreateBillingResponseDto
    │   └── data.url (link para pagar)
    │
    ▼
BillingProcessingService (cont.)
    │
    ├─ Parse response
    ├─ Create BillingTransaction
    ├─ Update BillingIntent.externalBillingId
    ├─ Update Order.status = PAID
    ├─ Publish OrderPaid event
    │
    ▼
PaymentController
    │
    ├─ Response 200 OK
    └── Body: BillingProcessingResult
        ├── billingId: String
        ├── paymentUrl: String
        ├── status: String
        └── externalTransactionId: String
```

---

## Estrutura de Pacotes

```
checkout/
│
├── interfaces/
│   └── payment/
│       └── PaymentController.java          ← REST API
│
├── application/
│   └── payment/
│       ├── BillingProcessingService.java   ← Use Case
│       ├── BillingIntentService.java       ← CRUD
│       └── BillingRequestMapper.java       ← Mapping
│
├── domain/
│   └── payment/
│       ├── entity/
│       │   ├── BillingIntent.java          ← Domain Model
│       │   └── BillingTransaction.java     ← Domain Model
│       │
│       ├── enums/
│       │   ├── BillingStatus.java          ← Enum
│       │   └── PaymentMethod.java          ← Enum
│       │
│       ├── dto/
│       │   ├── CreateBillingRequestDto.java    ✅ CRIADO
│       │   ├── CreateBillingResponseDto.java   ✅ CRIADO
│       │   ├── CreateBillingIntentRequestDto.java    ← TBD
│       │   └── BillingIntentResponseDto.java        ← TBD
│       │
│       ├── repository/
│       │   ├── BillingIntentRepository.java    ← TBD
│       │   └── BillingTransactionRepository.java   ← TBD
│       │
│       └── service/
│           ├── BillingIntentService.java       ← TBD
│           └── PaymentService.java             ← Existing
│
├── infrastructure/
│   ├── payment/
│   │   └── PaymentProviderClient.java      ← HTTP Client TBD
│   │
│   └── config/
│       └── PaymentProviderConfig.java      ← Configuration (partial)
│
├── common/
│   └── exception/
│       ├── GlobalExceptionHandler.java     ← Error handling
│       ├── PaymentDeclinedException.java   ← TBD
│       ├── DuplicateBillingException.java  ← TBD
│       └── GatewayTimeoutException.java    ← TBD
│
└── config/
    └── infrastructure/
        └── PaymentProviderConfig.java      ← WebClient config ✅
```

---

## Fluxo de Persistência

```
┌─────────────────────────────────────────────────────┐
│  BillingProcessingService                           │
│  - Recebe request                                   │
│  - Valida regras de negócio                         │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │ BillingIntentRepository     │
        │ save(BillingIntent)         │
        │ findByIdempotencyKey(key)   │
        │ findByOrderId(orderId)      │
        └─────────────────┬───────────┘
                          │
                          ▼
        ┌─────────────────────────────┐
        │ R2DBC Connection Pool       │
        │ Reactor Netty HTTP Client   │
        └─────────────────┬───────────┘
                          │
                          ▼
        ┌─────────────────────────────────────────┐
        │ MySQL                                   │
        │ INSERT INTO billing_intents (...)       │
        │ WHERE idempotency_key = UNIQUE          │
        └─────────────────────────────────────────┘
                          │
                          ▼
        ┌──────────────────────────────────┐
        │ Mono<BillingIntent>              │
        │ - Emitted after persist         │
        │ - Used for next step             │
        └──────────────────────────────────┘
                          │
                          ▼
        ┌─────────────────────────────────────────┐
        │ BillingTransactionRepository            │
        │ save(BillingTransaction)                │
        │ (persiste resposta do gateway)          │
        └─────────────────────────────────────────┘
```

---

## Tratamento de Erros

```
PaymentProviderClient
    │
    ├─ HTTP 4xx (Client Error)
    │   ├─ 400 Bad Request → InvalidBillingDataException
    │   ├─ 401 Unauthorized → UnauthorizedException
    │   ├─ 409 Conflict → DuplicateBillingException
    │   └─ 402 Payment Declined → PaymentDeclinedException
    │
    ├─ HTTP 5xx (Server Error)
    │   ├─ 500 Internal Server Error → GatewayException
    │   ├─ 502 Bad Gateway → GatewayUnavailableException
    │   └─ 503 Service Unavailable → CircuitBreakerOpen
    │
    ├─ Timeout
    │   └─ GatewayTimeoutException → 504 Gateway Timeout
    │
    ├─ Connection Error
    │   └─ CircuitBreaker fallback → 503 Service Unavailable
    │
    └─ Retry Logic
        ├─ Attempt 1: Immediate
        ├─ Attempt 2: Wait 500ms (exponential)
        ├─ Attempt 3: Wait 1000ms (exponential)
        └─ Fail: Throw exception after 3 attempts
```

---

## Padrões Implementados

```
🔄 Idempotency
   Request → externalId (unique)
   Response → Cached (se mesma requisição)
   Database → UNIQUE constraint on idempotency_key

🔁 Retry with Exponential Backoff
   Attempt 1: Immediate
   Attempt 2: 500ms
   Attempt 3: 1000ms
   Attempt 4: 2000ms (max)

🚧 Circuit Breaker
   Closed → Normal state
   Open → Fast fail (fallback)
   Half-Open → Single request to test recovery

📊 Correlation ID
   Header: X-Correlation-ID
   Propagates through all logs
   Enables distributed tracing

📝 Structured Logging
   All payment operations logged
   With: timestamp, correlation ID, billing ID, status
   No sensitive data (cards, tokens)

🎯 Reactive Programming (Non-blocking)
   WebClient (reactive)
   Mono<CreateBillingResponseDto>
   flatMap for composition
```

---

## Transações de Banco de Dados

```
BEGIN TRANSACTION
    │
    ├─ INSERT INTO billing_intents
    │  (idempotency_key = UNIQUE)
    │
    ├─ INSERT INTO billing_transactions
    │  (billing_intent_id FK)
    │
    ├─ UPDATE orders
    │  SET status = PAID
    │
    ├─ INSERT INTO outbox_events
    │  (event_type = 'OrderPaid')
    │
    └─ COMMIT
       (All or nothing)

ON ERROR:
    ROLLBACK
    (No partial state)
```

---

## Timeline de Implementação

```
SEMANA 1
├─ Dia 1 (2-4h)  : Task 1.2 - PaymentProviderClient
├─ Dia 1 (1h)    : Task 1.3 - PaymentProviderConfig update
├─ Dia 2-3 (3-4h): Tasks 2.1-2.4 - Entidades & Persistência
└─ Dia 3 (1-2h)  : Tasks 3.1-3.2 - Mappers

SEMANA 2
├─ Dia 1 (5-6h)  : Tasks 4.1-4.3 - Services
├─ Dia 2-3 (2-3h): Tasks 5.1-5.2 - Controllers
├─ Dia 3-4 (4-5h): Tasks 6.1-6.3 - Testes
└─ Dia 5 (1h)    : Task 7.1 - Documentação

TOTAL: 20-26 horas
SPRINT: 1 semana com 3+ desenvolvedores
        ou 2 semanas com 1-2 desenvolvedores
```

---

## Segurança & Conformidade

```
🔐 Autenticação
   JWT Token (5 minutos)
   Bearer <token> em Authorization header
   Validação em PaymentController

🔐 Autorização
   RBAC (Role-Based Access Control)
   CUSTOMER: Próprios dados apenas
   MERCHANT_ADMIN: Accesso extendido
   SUPPORT: Acesso total
   SYSTEM: Processos internos

🔐 Dados Sensíveis
   API Key → Env var (não em repo)
   Nunca logar tokens ou cartões
   Apenas salvar reference IDs do gateway

🔐 Conformidade
   PCI-DSS: Não armazenar dados de cartão
   HTTPS obrigatório
   Circuit breaker previne DDoS
```

---

**Versão:** 1.0  
**Data:** 19 de Fevereiro de 2026  
**Status:** Architecture Design Complete

