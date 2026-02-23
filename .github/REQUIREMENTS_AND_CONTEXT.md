# Payment Module — Análise de Requisitos & Contexto de Desenvolvimento

> **Atualizado em:** 23 de fevereiro de 2026
> **Escopo:** Este documento cobre **exclusivamente o módulo de pagamentos** do Payment Platform.
> É a fonte primária de contexto para sessões de IA. Deve ser lido antes de qualquer geração de código relacionada a pagamentos.

---

## 1. Visão Geral do Módulo

O módulo de pagamentos é responsável por:

- Criar e gerenciar **PaymentIntents** (intenção de cobrança)
- Integrar com o PSP **AbacatePay** para emissão de links/cobranças
- Registrar **PaymentTransactions** com o resultado de cada tentativa
- Gerenciar **webhooks** recebidos do AbacatePay para atualização de status
- Aplicar **idempotência** para evitar cobranças duplicadas
- Emitir **eventos de auditoria** para cada mudança de estado

**Pacote Java:** `checkout.domain.payment`
**Pacote do cliente HTTP:** `checkout.domain.client`
**Configuração do WebClient:** `checkout.config.infrastructure.PaymentProviderConfig`

---

## 2. Documentação Oficial AbacatePay

### 2.1 Metadados

| Campo | Valor |
|---|---|
| **Nome** | AbacatePay |
| **Versão** | v1 |
| **Base URL** | `https://api.abacatepay.com` |
| **Autenticação** | Bearer API Key — header `Authorization: Bearer <API_KEY>` |
| **Modo Dev** | Chaves criadas em modo dev processam transações de teste |
| **Modo Produção** | Chaves de produção processam transações reais |

### 2.2 Conceitos Fundamentais

#### Cliente (Customer)
Usuário final que será cobrado. Estrutura retornada pela API:

```json
{
  "id": "cust_xxxxx",
  "metadata": {
    "name": "Customer Name",
    "cellphone": "11999999999",
    "taxId": "12345678900",
    "email": "email@example.com"
  }
}
```

#### Billing (Cobrança)
Cobrança ou link de pagamento que permite ao cliente efetuar o pagamento.

- `ONE_TIME` — pagamento único
- `MULTIPLE_PAYMENTS` — múltiplos pagamentos

#### Métodos de Pagamento Suportados
- `PIX`
- `CARD`

> ⚠️ **Atenção:** O AbacatePay suporta apenas `PIX` e `CARD`. O domínio interno usa `PIX`, `CREDIT_CARD`, `DEBIT_CARD`, `BOLETO` — o mapper deve converter para os valores suportados pelo PSP.

### 2.3 Endpoints AbacatePay

#### Billing

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/v1/billing/create` | Criar nova cobrança |
| `GET` | `/v1/billing/list` | Listar cobranças |

**Request Body — `POST /v1/billing/create`:**

```json
{
  "frequency": "ONE_TIME",
  "methods": ["PIX"],
  "amount": 1000,
  "description": "Pagamento teste",
  "customer": {
    "name": "Cliente",
    "email": "cliente@email.com"
  }
}
```

#### PIX QR Code

| Método | Path | Query Params | Descrição |
|---|---|---|---|
| `POST` | `/v1/pix/qrcode/create` | — | Gerar QR Code PIX |
| `GET` | `/v1/pix/qrcode/check` | `id: string (required)` | Consultar status do QR Code |
| `POST` | `/v1/pix/qrcode/simulate-payment` | — | Simular pagamento (apenas modo dev) |

#### Customer

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/v1/customer/create` | Criar cliente |
| `GET` | `/v1/customer/list` | Listar clientes |

#### Coupons

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/v1/coupon/create` | Criar cupom |
| `GET` | `/v1/coupon/list` | Listar cupons |

#### Merchant

| Método | Path | Descrição |
|---|---|---|
| `GET` | `/v1/merchant/info` | Informações do merchant |
| `GET` | `/v1/merchant/mrr` | Receita recorrente mensal (MRR) |

#### Withdraw

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/v1/withdraw/create` | Solicitar saque |

### 2.4 Webhooks AbacatePay

O AbacatePay envia eventos HTTP POST para a URL configurada no painel quando o status de uma cobrança muda.

**Eventos possíveis:**

| Evento | Significado |
|---|---|
| `PAID` | Pagamento confirmado — estado terminal de sucesso |
| `PENDING` | Aguardando pagamento |
| `EXPIRED` | Cobrança expirada — estado terminal de falha |
| `CANCELLED` | Cobrança cancelada — estado terminal de falha |

### 2.5 Boas Práticas AbacatePay

**Segurança:**
- Armazenar API keys em variáveis de ambiente (nunca hardcoded)
- Não expor chaves no frontend
- Revogar chaves comprometidas imediatamente

**Idempotência:**
- As operações são idempotentes para evitar cobranças duplicadas

---

## 3. Estado Atual da Implementação

### 3.1 O Que Está Implementado ✅

#### Infraestrutura / Configuração

| Classe | Pacote | Status | Observações |
|---|---|---|---|
| `PaymentProviderConfig` | `config.infrastructure` | ✅ Completo | WebClient bean `paymentProviderWebClient` com TLS, compressão, pool de conexões (max 100) |
| `PaymentServiceClient` | `domain.client` | ✅ Completo | Implementa apenas `POST /v1/billing/create` |

**`PaymentProviderConfig` — detalhes:**
- Bean: `paymentProviderWebClient`
- Max connections: `100`
- Pending acquire max: `1000`
- Pending acquire timeout: `45s`
- Max idle time: `20s`
- Response timeout: variável `ABACATEPAY_TIMEOUT_MS`
- TLS: habilitado (`.secure()`)
- Compressão: habilitada
- Headers padrão: `Authorization: Bearer`, `Content-Type: application/json`, `User-Agent: MagaluPaymentService/1.0`

#### Entidades (Spring Data R2DBC)

| Entidade | Tabela | Status | Campos |
|---|---|---|---|
| `PaymentIntent` | `payment_intents` | ✅ Completo | `id`, `orderId`, `amount` (BigDecimal), `currency` (default BRL), `status` (default PENDING), `paymentMethod`, `idempotencyKey`, `createdAt` |
| `PaymentTransaction` | `payment_transactions` | ✅ Completo | `id`, `paymentIntentId`, `externalId`, `status`, `failureReason`, `processedAt`, `createdAt` |

**Status válidos — `PaymentIntent`:** `PENDING` → `PROCESSING` → `APPROVED` \| `DENIED` \| `REFUNDED`
**Status válidos — `PaymentTransaction`:** `PENDING`, `PROCESSING`, `APPROVED`, `DENIED`, `REFUNDED`

#### Repositories

| Interface | Status | Métodos customizados |
|---|---|---|
| `PaymentIntentRepository` | ✅ Completo | `findByIdempotencyKey`, `findByOrderId`, `existsByIdempotencyKey` |
| `PaymentTransactionRepository` | ✅ Completo | `findByPaymentIntentId`, `findByExternalId` |

#### DTOs

| Classe DTO | Direção | Status | Propósito |
|---|---|---|---|
| `CreateBillingRequestDto` | Interno: API → Service | ✅ Completo | Contrato de entrada da nossa API — valida `methods`, `products`, `returnUrl`, `completionUrl` |
| `CreateBillingRequestExtDto` | Externo: Service → AbacatePay | ✅ Completo | Mapeado para o contrato exato do AbacatePay — inclui `frequency`, `methods`, `products`, `customer`, `customerId`, `externalId`, `metadata` |
| `CreateBillingResponseExtDto` | Externo: AbacatePay → Service | ✅ Completo | Deserializa resposta completa: `data.id`, `data.url`, `data.status`, `data.methods`, `data.products`, `data.customer.metadata`, `data.amount` (deprecated), `error` |
| `CreateBillingResponseDto` | Interno: Service → API client | ✅ Completo | Expõe: `paymentIntentId`, `status`, `paymentUrl`, `paymentMethods`, `amountInCents` |
| `CustomerExtDto` | Externo: outbound | ✅ Completo | `name`, `cellphone`, `email`, `taxId` |
| `ProductExtDto` | Externo: outbound | ✅ Completo | `externalId`, `name`, `description`, `quantity`, `price` (cents, mínimo 100) |

#### Mapper

| Classe | Status | Métodos |
|---|---|---|
| `PaymentMapper` | ✅ Completo | `toExternalRequest(CreateBillingRequestDto)` → `CreateBillingRequestExtDto`; `toPaymentResponseDto(PaymentIntent, CreateBillingResponseExtDto)` → `CreateBillingResponseDto` |

**⚠️ Problema identificado no mapper:** `toPaymentResponseDto` usa `data.getAmount()` que está marcado como `@Deprecated` no DTO. O valor correto deve ser calculado como `sum(products[].price * products[].quantity)`.

#### Migração de Banco

| Migration | Tabelas | Status |
|---|---|---|
| `V4__create_payments.sql` | `payment_intents`, `payment_transactions` | ✅ Schema criado |

Constraints do schema:
- `payment_method CHECK IN ('PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'BOLETO')`
- `status CHECK IN ('PENDING', 'PROCESSING', 'APPROVED', 'DENIED', 'REFUNDED')`
- `idempotency_key UNIQUE`
- `amount > 0`
- FK: `order_id → orders(id) ON DELETE RESTRICT`
- FK: `payment_intent_id → payment_intents(id) ON DELETE CASCADE`

---

### 3.2 O Que Está Faltando ❌

#### 3.2.1 `PaymentService` — Lógica de Negócio (CRÍTICO)

**Arquivo:** `checkout/domain/payment/service/PaymentService.java`

O arquivo existe mas está **completamente vazio** — apenas declara as dependências. Falta:

- Anotação `@Service` (ausente — o bean não é registrado no Spring)
- Implementação de todos os métodos de negócio

**Métodos a implementar:**

```java
// 1. Criação de billing com idempotência
Mono<CreateBillingResponseDto> createBilling(
    CreateBillingRequestDto request,
    String idempotencyKey,
    Long authenticatedUserId
)

// 2. Consulta de PaymentIntent por ID
Mono<PaymentIntent> getPaymentIntentById(Long id)

// 3. Consulta de PaymentIntent por Order
Mono<PaymentIntent> getPaymentIntentByOrderId(Long orderId)

// 4. Processamento do webhook do AbacatePay
Mono<Void> processWebhookEvent(String billingId, String newStatus)

// 5. Listagem de PaymentTransactions de um intent
Flux<PaymentTransaction> getTransactionsByPaymentIntentId(Long paymentIntentId)
```

**Fluxo completo de `createBilling`:**
```
1. Verificar idempotência: existsByIdempotencyKey(idempotencyKey)
   └─ Se existir → retornar resposta cacheada (via IdempotencyService — pendente)

2. Validar que o Order existe e pertence ao cliente autenticado
   └─ OrderRepository.findById (módulo Order — pendente)

3. Executar RiskAnalysis (módulo Risk — pendente)
   ├─ DENY  → lançar RiskAnalysisDeniedException
   ├─ REVIEW → persistir, retornar 202 Accepted
   └─ APPROVE → continuar

4. Persistir PaymentIntent (status = PENDING)

5. Mapear request interno → CreateBillingRequestExtDto via PaymentMapper
   └─ Injetar dados do Customer (name, email, cellphone, taxId) em customer field

6. Chamar PaymentServiceClient.createBilling(extRequest)
   ├─ Sucesso:
   │   ├─ Atualizar PaymentIntent.status = PROCESSING
   │   ├─ Persistir PaymentTransaction (externalId = data.id do billing)
   │   └─ Retornar CreateBillingResponseDto (url para redirecionar o cliente)
   └─ Erro:
       ├─ Atualizar PaymentIntent.status = DENIED
       ├─ Persistir PaymentTransaction (failureReason = mensagem do erro)
       ├─ Emitir AuditLog (pendente)
       └─ Propagar erro mapeado

7. Persistir IdempotencyKey com resposta serializada (pendente)

8. Emitir OutboxEvent: PAYMENT_INTENT_CREATED (pendente)
```

**Fluxo de `processWebhookEvent`:**
```
1. Buscar PaymentTransaction pelo externalId (billingId do AbacatePay)
2. Atualizar PaymentTransaction.status conforme evento
3. Atualizar PaymentIntent.status:
   ├─ PAID       → APPROVED
   ├─ EXPIRED    → DENIED
   └─ CANCELLED  → DENIED
4. Se APPROVED → atualizar Order.status = PAID (via OrderService — pendente)
5. Emitir OutboxEvent (pendente)
6. Emitir AuditLog (pendente)
```

---

#### 3.2.2 `PaymentController` — Endpoint REST (CRÍTICO)

**Arquivo a criar:** `checkout/domain/payment/controller/PaymentController.java`

Endpoints a implementar:

| Método | Path | Role | Body / Params | Response |
|---|---|---|---|---|
| `POST` | `/api/payments/billing` | `CUSTOMER`, `MERCHANT_ADMIN` | `CreateBillingRequestDto` + header `Idempotency-Key` | `201 Created` + `CreateBillingResponseDto` |
| `GET` | `/api/payments/{id}` | `CUSTOMER`, `SUPPORT`, `MERCHANT_ADMIN` | `{id}` path var | `200 OK` + `PaymentIntentResponseDto` |
| `GET` | `/api/payments/order/{orderId}` | `CUSTOMER`, `SUPPORT` | `{orderId}` path var | `200 OK` + `PaymentIntentResponseDto` |
| `GET` | `/api/payments/{id}/transactions` | `SUPPORT`, `MERCHANT_ADMIN` | `{id}` path var | `200 OK` + `List<PaymentTransactionResponseDto>` |
| `POST` | `/api/payments/webhook` | Público + validação HMAC | Payload AbacatePay | `200 OK` |

**Regras de segurança:**
- CUSTOMER só acessa próprios pagamentos (validar via `orderId → customerId → userId`)
- `/api/payments/webhook` deve ser público mas validado via HMAC-SHA256

---

#### 3.2.3 `PaymentIntentResponseDto` e `PaymentTransactionResponseDto` — DTOs de saída (CRÍTICO)

**Arquivos a criar:**
- `checkout/domain/payment/dto/PaymentIntentResponseDto.java`
- `checkout/domain/payment/dto/PaymentTransactionResponseDto.java`

Campos esperados em `PaymentIntentResponseDto`:
```java
Long id
Long orderId
BigDecimal amount
String currency        // "BRL"
String status          // PENDING | PROCESSING | APPROVED | DENIED | REFUNDED
String paymentMethod
String idempotencyKey
LocalDateTime createdAt
```

---

#### 3.2.4 `PaymentServiceClient` — Endpoints AbacatePay não implementados

**Arquivo existente:** `checkout/domain/client/PaymentServiceClient.java`

Atualmente só implementa `POST /v1/billing/create`. Os seguintes métodos precisam ser adicionados:

| Método AbacatePay | Prioridade | Para que serve |
|---|---|---|
| `GET /v1/billing/list` | Média | Listar cobranças do merchant |
| `POST /v1/pix/qrcode/create` | Alta | Criar QR Code PIX diretamente (alternativa ao billing link) |
| `GET /v1/pix/qrcode/check?id=` | Alta | Verificar status de um QR Code PIX |
| `POST /v1/pix/qrcode/simulate-payment` | Baixa (dev only) | Simular pagamento em ambiente de testes |
| `POST /v1/customer/create` | Alta | Registrar cliente no AbacatePay antes de criar billing |
| `GET /v1/customer/list` | Baixa | Listar clientes cadastrados |

> **Nota:** O endpoint `POST /v1/customer/create` é importante para pré-registrar o cliente no AbacatePay e reutilizar o `customerId` em billings futuras, evitando recriar o cliente a cada cobrança.

---

#### 3.2.5 `PaymentMapper` — Correções e Extensões

**Arquivo existente:** `checkout/domain/payment/mapper/PaymentMapper.java`

**Problemas a corrigir:**

1. **`toPaymentResponseDto` usa campo deprecated `data.getAmount()`** — substituir pelo cálculo correto: `sum(products[i].price * products[i].quantity)`

2. **`toExternalRequest` não injeta dados do Customer** — o método precisa receber um `CustomerExtDto` como parâmetro e populá-lo no request externo:
```java
// Assinatura atual (incompleta):
public CreateBillingRequestExtDto toExternalRequest(CreateBillingRequestDto request)

// Assinatura correta:
public CreateBillingRequestExtDto toExternalRequest(
    CreateBillingRequestDto request,
    CustomerExtDto customer,    // dados do cliente para AbacatePay
    String externalId           // orderId ou paymentIntentId para reconciliação
)
```

3. **Adicionar mapper** para `PaymentIntent → PaymentIntentResponseDto`

4. **Adicionar mapper** para `PaymentTransaction → PaymentTransactionResponseDto`

---

#### 3.2.6 Webhook Handler — Validação HMAC

**Arquivo a criar:** `checkout/domain/payment/webhook/WebhookValidator.java` (ou dentro do controller)

AbacatePay envia um header (ex.: `X-AbacatePay-Signature`) com assinatura HMAC-SHA256 do payload. A implementação deve:

```java
// Lógica de validação
boolean isValidSignature(String payload, String receivedSignature, String secret) {
    String expected = HmacUtils.hmacSha256Hex(secret, payload);
    return MessageDigest.isEqual(expected.getBytes(), receivedSignature.getBytes());
}
```

> ⚠️ Usar `MessageDigest.isEqual` (comparação em tempo constante) para evitar timing attacks.

---

#### 3.2.7 Atualização do `SecurityConfig`

**Arquivo existente:** `checkout/config/security/SecurityConfig.java`

Adicionar liberação do endpoint de webhook:
```java
.pathMatchers("/api/payments/webhook").permitAll()  // validado via HMAC
```

---

#### 3.2.8 Dependências do Módulo de Pagamentos (outros módulos pendentes)

O `PaymentService` depende de módulos ainda não implementados. Esses módulos bloqueiam a implementação completa:

| Dependência | Módulo | Status | Necessário para |
|---|---|---|---|
| `OrderRepository` | Order Domain | ❌ Não implementado | Validar que o pedido existe antes de cobrar |
| `CustomerService.getCustomerById` | Customer Domain | ✅ Implementado | Obter dados do cliente para injetar no billing |
| `RiskAnalysisService` | Risk Domain | ❌ Não implementado | Análise de fraude antes de cobrar |
| `IdempotencyService` | Idempotency Domain | ❌ Não implementado | Evitar cobranças duplicadas |
| `AuditLogService` | Audit Domain | ❌ Não implementado | Registrar eventos de pagamento |
| `OutboxEventService` | Event Domain | ❌ Não implementado | Publicar eventos de mudança de estado |

---

## 4. Arquivos Existentes — Inventário Completo do Módulo

```
checkout/
├── config/
│   └── infrastructure/
│       └── PaymentProviderConfig.java         ✅ WebClient configurado
│
├── domain/
│   ├── client/
│   │   └── PaymentServiceClient.java          ✅ createBilling implementado
│   │                                          ❌ outros endpoints AbacatePay não implementados
│   │
│   └── payment/
│       ├── dto/
│       │   ├── CreateBillingRequestDto.java    ✅ DTO interno de entrada
│       │   ├── CreateBillingRequestExtDto.java ✅ DTO externo para AbacatePay
│       │   ├── CreateBillingResponseDto.java   ✅ DTO interno de saída
│       │   ├── CreateBillingResponseExtDto.java✅ DTO de resposta do AbacatePay
│       │   ├── CustomerExtDto.java             ✅ Customer para AbacatePay
│       │   └── ProductExtDto.java              ✅ Product para AbacatePay
│       │
│       │   ❌ PaymentIntentResponseDto.java    (faltando)
│       │   ❌ PaymentTransactionResponseDto.java (faltando)
│       │
│       ├── entity/
│       │   ├── PaymentIntent.java              ✅ Entidade R2DBC completa
│       │   └── PaymentTransaction.java         ✅ Entidade R2DBC completa
│       │
│       ├── mapper/
│       │   └── PaymentMapper.java              ⚠️ Existe mas tem problemas (ver §3.2.5)
│       │
│       ├── repository/
│       │   ├── PaymentIntentRepository.java    ✅ Completo
│       │   └── PaymentTransactionRepository.java ✅ Completo
│       │
│       ├── service/
│       │   └── PaymentService.java             ❌ Skeleton vazio, sem @Service
│       │
│       └── controller/
│           ❌ PaymentController.java           (faltando)
│           ❌ WebhookValidator.java            (faltando)
```

---

## 5. Variáveis de Ambiente do Módulo de Pagamentos

| Variável | Uso | Obrigatória |
|---|---|---|
| `ABACATEPAY_API_URL` | Base URL do AbacatePay (`https://api.abacatepay.com`) | ✅ |
| `PSP_API_KEY` | Bearer token da API AbacatePay | ✅ |
| `ABACATEPAY_TIMEOUT_MS` | Timeout do WebClient em ms (default: `5000`) | ✅ (tem fallback) |
| `ABACATEPAY_WEBHOOK_SECRET` | Segredo para validação HMAC de webhooks | ❌ Pendente de implementação |

> A variável `ABACATEPAY_WEBHOOK_SECRET` ainda não existe no código — deve ser adicionada ao `application.properties` e ao `PaymentProviderConfig` (ou em uma nova classe de configuração).

---

## 6. Schema do Banco de Dados (Referência)

### `payment_intents`
```sql
id               BIGINT AUTO_INCREMENT PRIMARY KEY
order_id         BIGINT NOT NULL  -- FK → orders(id) ON DELETE RESTRICT
amount           DECIMAL(19,2) NOT NULL  -- sempre > 0
currency         VARCHAR(3) DEFAULT 'BRL'
status           VARCHAR(20) DEFAULT 'PENDING'
                 CHECK IN ('PENDING','PROCESSING','APPROVED','DENIED','REFUNDED')
payment_method   VARCHAR(20) NOT NULL
                 CHECK IN ('PIX','CREDIT_CARD','DEBIT_CARD','BOLETO')
idempotency_key  VARCHAR(255) NOT NULL UNIQUE
created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP

Indexes: order_id, status, idempotency_key, payment_method, created_at
```

### `payment_transactions`
```sql
id                 BIGINT AUTO_INCREMENT PRIMARY KEY
payment_intent_id  BIGINT NOT NULL  -- FK → payment_intents(id) ON DELETE CASCADE
external_id        VARCHAR(255)     -- ID da cobrança no AbacatePay (ex: "bill_xxxxx")
status             VARCHAR(20) NOT NULL
                   CHECK IN ('PENDING','PROCESSING','APPROVED','DENIED','REFUNDED')
failure_reason     TEXT
processed_at       TIMESTAMP
created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP

Indexes: payment_intent_id, external_id, status, processed_at
```

---

## 7. Backlog Priorizado — Módulo de Pagamentos

### 🔴 Prioridade 1 — Bloqueante

| # | Tarefa | Arquivo | Observação |
|---|---|---|---|
| P1.1 | Adicionar `@Service` ao `PaymentService` | `PaymentService.java` | Bean não é registrado sem essa annotation |
| P1.2 | Implementar `createBilling` no `PaymentService` | `PaymentService.java` | Core do módulo |
| P1.3 | Criar `PaymentController` com endpoint `POST /api/payments/billing` | `PaymentController.java` (novo) | |
| P1.4 | Corrigir `PaymentMapper.toExternalRequest` para injetar Customer | `PaymentMapper.java` | Sem isso o billing não envia dados do cliente |
| P1.5 | Corrigir uso do campo `@Deprecated amount` no `PaymentMapper` | `PaymentMapper.java` | Calcular total via produtos |

### 🟡 Prioridade 2 — Essencial para Produção

| # | Tarefa | Arquivo | Observação |
|---|---|---|---|
| P2.1 | Criar `PaymentIntentResponseDto` e `PaymentTransactionResponseDto` | `dto/` | |
| P2.2 | Implementar endpoint `GET /api/payments/{id}` | `PaymentController.java` | |
| P2.3 | Implementar endpoint `POST /api/payments/webhook` com HMAC | `PaymentController.java` + `WebhookValidator.java` | |
| P2.4 | Implementar `processWebhookEvent` no `PaymentService` | `PaymentService.java` | Atualiza status após pagamento |
| P2.5 | Adicionar `ABACATEPAY_WEBHOOK_SECRET` ao `application.properties` | `application.properties` | |
| P2.6 | Liberar `/api/payments/webhook` no `SecurityConfig` | `SecurityConfig.java` | |
| P2.7 | Implementar `POST /v1/customer/create` no `PaymentServiceClient` | `PaymentServiceClient.java` | Pré-registro do cliente no AbacatePay |

### 🟢 Prioridade 3 — Qualidade e Completude

| # | Tarefa | Arquivo | Observação |
|---|---|---|---|
| P3.1 | Implementar `GET /v1/pix/qrcode/create` no `PaymentServiceClient` | `PaymentServiceClient.java` | Alternativa ao billing link |
| P3.2 | Implementar `GET /v1/pix/qrcode/check` no `PaymentServiceClient` | `PaymentServiceClient.java` | Polling de status |
| P3.3 | Implementar `GET /api/payments/order/{orderId}` no controller | `PaymentController.java` | |
| P3.4 | Implementar `GET /api/payments/{id}/transactions` no controller | `PaymentController.java` | |
| P3.5 | Testes unitários do `PaymentService` com Mockito + StepVerifier | `test/` | |
| P3.6 | Testes de integração do `PaymentController` com Testcontainers | `test/` | Mock do AbacatePay via WireMock |
| P3.7 | Adicionar `POST /v1/pix/qrcode/simulate-payment` (dev mode) | `PaymentServiceClient.java` | Só expor em profile dev |

---

## 8. Regras de Negócio — Módulo de Pagamentos

### Estado do PaymentIntent
```
PENDING ──► PROCESSING ──► APPROVED  (estado final — pagamento confirmado)
                │
                └──────────► DENIED   (estado final — pagamento rejeitado/expirado)

APPROVED ──► REFUNDED  (estado final — estorno)
```

### Regras de Transição
- `PENDING → PROCESSING`: quando o AbacatePay retorna sucesso na criação do billing
- `PROCESSING → APPROVED`: quando webhook `PAID` é recebido do AbacatePay
- `PROCESSING → DENIED`: quando webhook `EXPIRED` ou `CANCELLED` é recebido
- `APPROVED → REFUNDED`: quando estorno é solicitado (implementação futura)
- Transições inválidas devem lançar `InvalidPaymentStatusException`

### Idempotência
- Toda requisição de criação de billing DEVE conter header `Idempotency-Key`
- Mesma chave + mesmo método + mesmo path → retornar resposta cacheada (HTTP 200)
- Chaves expiram em 24 horas
- Chaves são case-sensitive

### Mapeamento de Métodos de Pagamento (Interno → AbacatePay)
| Interno | AbacatePay |
|---|---|
| `PIX` | `PIX` |
| `CREDIT_CARD` | `CARD` |
| `DEBIT_CARD` | `CARD` |
| `BOLETO` | ❌ Não suportado pelo AbacatePay — lançar `PaymentMethodNotSupportedException` |

### Valores Monetários
- Internamente: `BigDecimal` em BRL (ex.: `29.90`)
- AbacatePay: `Integer` em centavos (ex.: `2990`)
- Conversão: `amount.multiply(BigDecimal.valueOf(100)).intValue()`
- Valor mínimo: R$ 1,00 (100 centavos)

### Dados do Cliente no Billing
- O campo `customer` no request para o AbacatePay deve ser populado com dados do `Customer` do nosso sistema
- Mapeamento: `Customer.name → name`, `Customer.email → email`, `Customer.phoneNumber → cellphone`, `Customer.document.documentNumber → taxId`
- Se o AbacatePay retornar um `customerId` na resposta, armazená-lo para reuso futuro (campo não existe ainda na entidade `Customer` — adicionar `abacatePayCustomerId VARCHAR(50)` via nova migration)

---

## 9. Convenções de Código — Módulo de Pagamentos

```java
// ✅ Correto — chain reativo legível
public Mono<CreateBillingResponseDto> createBilling(CreateBillingRequestDto request, String idempotencyKey) {
    return checkIdempotency(idempotencyKey)
        .switchIfEmpty(
            persistPaymentIntent(request)
                .flatMap(intent -> callAbacatePay(intent, request))
                .flatMap(result -> persistTransaction(result))
                .flatMap(result -> cacheIdempotencyKey(idempotencyKey, result))
        );
}

// ❌ Errado — nunca usar .block() em cadeia reativa
PaymentIntent intent = paymentIntentRepository.save(entity).block();

// ✅ Correto — valores monetários
BigDecimal amount = new BigDecimal("29.90");        // armazenamento interno
int amountInCents = amount.multiply(BigDecimal.valueOf(100)).intValue();  // para AbacatePay

// ✅ Correto — logging estruturado
log.info("Creating billing. orderId={}, idempotencyKey={}, method={}", orderId, idempotencyKey, method);
log.error("AbacatePay returned error. status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
```

---

## 10. Problemas Conhecidos (Technical Debt)

| Severidade | Problema | Localização | Ação Necessária |
|---|---|---|---|
| 🔴 Crítico | `PaymentService` sem `@Service` | `PaymentService.java:10` | Adicionar `@Service` |
| 🔴 Crítico | `PaymentService` sem nenhum método | `PaymentService.java` | Implementar toda lógica de negócio |
| 🔴 Crítico | `PaymentController` não existe | `domain/payment/controller/` | Criar do zero |
| 🟡 Médio | `PaymentMapper` usa `@Deprecated amount` | `PaymentMapper.java:62` | Calcular total via soma dos produtos |
| 🟡 Médio | `PaymentMapper.toExternalRequest` não injeta Customer | `PaymentMapper.java:28` | Adicionar parâmetro `CustomerExtDto` |
| 🟡 Médio | Webhook sem validação HMAC | Não implementado | Criar `WebhookValidator` |
| 🟡 Médio | `ABACATEPAY_WEBHOOK_SECRET` não configurado | `application.properties` | Adicionar variável |
| 🟡 Médio | `Customer` não armazena `abacatePayCustomerId` | `Customer.java` + DB | Nova migration V10 |
| 🟢 Baixo | `PaymentServiceClient` cobre apenas 1 dos 10 endpoints do AbacatePay | `PaymentServiceClient.java` | Adicionar endpoints conforme prioridade |
| 🟢 Baixo | Sem testes para o módulo de pagamentos | `test/` | Criar testes unitários e de integração |

---

*Este documento deve ser atualizado sempre que um item do backlog for concluído ou quando decisões arquiteturais relevantes forem tomadas no módulo de pagamentos.*

