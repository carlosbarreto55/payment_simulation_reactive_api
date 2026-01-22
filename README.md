# Plataforma de Checkout e Pagamentos Reativa

#DISCLAIMER: PROJETO EM FASE INICIAL DE DESENVOLVIMENTO!

Projeto de portfólio em desenvolvimento, inspirado em sistemas reais de checkout e pagamentos utilizados por grandes empresas.
## Objetivo

Construir um backend reativo que represente um **sistema completo de checkout e pagamentos**, cobrindo:

- ✅ Criação e gestão de pedidos
- ✅ Processamento de pagamentos (PIX, cartão, boleto – simulados)
- ✅ Análise de risco/antifraude
- ✅ Webhooks e eventos assíncronos
- ✅ Autenticação e autorização robustas (JWT)
- ✅ Pipeline de CI/CD com GitHub Actions
- ✅ Idempotência em operações críticas
- ✅ Auditoria e observabilidade

## Arquitetura

O projeto segue uma arquitetura de **monólito modular**, padrão muito utilizado em empresas grandes:

```
┌────────────┐
│   Client   │
└─────┬──────┘
      ↓
┌─────────────────────────────┐
│   API Gateway (WebFlux)     │
├─────────┬─────────┬─────────┤
│  Auth   │ Orders  │ Payments│
├─────────┼─────────┼─────────┤
│  Risk   │ Notify  │ Webhooks│
└─────────┴─────────┴─────────┘
      ↓
┌─────────────────────────────┐
│   MySQL (R2DBC + Flyway)    │
└─────────────────────────────┘
```

### Características Arquiteturais

- **Programação Reativa**: Uso de Reactor para operações não-bloqueantes
- **Outbox Pattern**: Garantia de entrega de eventos assíncronos
- **Idempotência**: Prevenção de cobranças duplicadas
- **RBAC**: Controle de acesso baseado em roles
- **Separação por Domínio**: Organização clara por contexto de negócio

## Tecnologias Utilizadas

### Backend
- **Java 17+**
- **Spring Boot 3.5.3**
- **Spring WebFlux** (Programação Reativa)
- **Spring Security** (JWT Authentication)
- **Spring Data R2DBC** (MySQL reativo)
- **Flyway** (Migrações de banco de dados)
- **Lombok** (Redução de boilerplate)

### Banco de Dados
- **MySQL 8.0+**
- **R2DBC MySQL Driver** (Driver reativo)

### Segurança
- **JWT** (JSON Web Tokens) - Access + Refresh Token
- **Spring Security WebFlux**
- **HMAC** (Assinatura de webhooks)


### Testes
- **JUnit 5**
- **Reactor Test**
- **Testcontainers** (MySQL para testes)
- **WebTestClient**

### DevOps
- **Docker** + **Docker Compose**
- **GitHub Actions** (CI/CD)

## Domínios do Sistema

### Identity & Auth
- Registro e login de usuários
- JWT com access e refresh tokens
- Perfis de acesso:
  - `CUSTOMER` - Cliente final
  - `MERCHANT_ADMIN` - Administrador de loja
  - `SUPPORT` - Suporte técnico
  - `SYSTEM` - Sistema interno

### Customers & Merchants
- Gestão de clientes (KYC simplificado)
- Gestão de lojistas e suas lojas
- Relacionamento cliente → pedido → pagamento

### Orders
- Criação de pedidos
- Estados do pedido:
  - `CREATED` - Criado
  - `RESERVED` - Estoque reservado
  - `PAID` - Pago
  - `CANCELED` - Cancelado
  - `REFUNDED` - Estornado
- Reserva de estoque (simulada)
- Cancelamento seguro

### Payments (Core)
- Criação de **Payment Intent**
- Métodos de pagamento:
  - **PIX**
  - **Cartão de Crédito** (simulado)
  - **Cartão de Débito** (simulado)
  - **Boleto** (simulado)
- **Idempotency-Key** obrigatória
- Prevenção de cobrança duplicada
- Suporte a estorno

## Regras de Negócio Principais

### Autenticação & Acesso
- Um usuário só pode acessar pedidos que ele criou, exceto:
  - `SUPPORT` → pode apenas **visualizar** qualquer pedido
  - `MERCHANT_ADMIN` → pode visualizar pedidos da própria loja
- Refresh token inválido invalida automaticamente todos os tokens ativos do usuário
- Tentativas excessivas de login bloqueiam temporariamente o usuário

### Pedidos
- Um pedido só pode ser pago se estiver no estado `CREATED` ou `RESERVED`
- Um pedido `PAID` **não pode** ser cancelado diretamente — apenas estornado
- Se a reserva de estoque falhar, o pedido é automaticamente cancelado
- Um pedido só pode ter **um pagamento ativo por vez**

### Pagamentos
- Toda requisição de criação/confirmação de pagamento **exige Idempotency-Key**
- Requisições idempotentes retornam sempre a **mesma resposta**, sem duplicar transações
- Um pagamento aprovado:
  - Atualiza o pedido para `PAID`
  - Gera evento de notificação
- Um pagamento negado:
  - Mantém o pedido em `CREATED`
  - Registra motivo técnico e de negócio
- Estornos só são permitidos para pagamentos `APPROVED`

### Webhooks & Eventos
- Webhooks devem conter assinatura HMAC válida
- Webhooks são **idempotentes** (reprocessamento seguro)
- Falhas de entrega não perdem eventos (Outbox Pattern)
- Eventos são entregues **pelo menos uma vez**

## Como Executar

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6+
- Docker e Docker Compose (opcional, mas recomendado)
- MySQL 8.0+ (se não usar Docker)

### Opção 1: Com Docker Compose (Recomendado)

1. Clone o repositório:
```bash
git clone <repository-url>
cd magalu
```

2. Execute o Docker Compose:
```bash
docker-compose up -d
```

Isso irá iniciar:
- MySQL na porta 3306
- Aplicação Spring Boot na porta 8080

3. Acesse a aplicação:
```
http://localhost:8080
```

### Opção 2: Execução Local

1. Configure o MySQL:
```sql
CREATE DATABASE magalu_db;
CREATE USER 'root'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON magalu_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

2. Configure o `application.properties` com suas credenciais:
```properties
spring.r2dbc.url=r2dbc:mysql://localhost:3306/magalu_db
spring.r2dbc.username=root
spring.r2dbc.password=sua_senha
```

3. Execute a aplicação:
```bash
./mvnw spring-boot:run
```


## Endpoints Principais

### Autenticação
- `POST /api/auth/register` - Registrar novo usuário
- `POST /api/auth/login` - Fazer login
- `POST /api/auth/refresh` - Renovar access token
- `POST /api/auth/logout` - Fazer logout

### Pedidos
- `POST /api/orders` - Criar pedido
- `GET /api/orders/{id}` - Buscar pedido
- `POST /api/orders/{id}/cancel` - Cancelar pedido

### Pagamentos
- `POST /api/payments/intents` - Criar payment intent
- `POST /api/payments/{id}/confirm` - Confirmar pagamento
- `POST /api/payments/{id}/refund` - Estornar pagamento

### Antifraude
- `POST /api/risk/analyze` - Analisar risco
- `POST /api/risk/{id}/approve` - Aprovar manualmente (SUPPORT)

### Webhooks
- `POST /api/webhooks/config` - Configurar webhook
- `GET /api/webhooks/deliveries` - Listar entregas

## Testes

### Executar Testes
```bash
./mvnw test
```

### Executar Testes com Cobertura
```bash
./mvnw test jacoco:report
```

### Testes de Integração
Os testes de integração utilizam **Testcontainers** para criar um ambiente MySQL isolado.

## CI/CD

O projeto possui pipeline de CI/CD configurado com **GitHub Actions**:

1. **Pull Request**: Executa build e testes
2. **Build**: Gera imagem Docker e faz push para GHCR
3. **Deploy**: Deploy automático para staging, manual para produção

## 📊 Observabilidade

### Métricas Prometheus
```
http://localhost:8080/actuator/prometheus
```

### Health Check
```
http://localhost:8080/actuator/health
```

### Métricas
```
http://localhost:8080/actuator/metrics
```

## Segurança

- **JWT** com refresh token
- **Spring Security WebFlux**
- **Rate limiting** em endpoints sensíveis
- **Mascaramento de dados sensíveis** em logs
- **Assinatura HMAC** em webhooks
- **Validação de entrada** em todos os endpoints

## Padrões Aplicados

- ✅ **Programação Reativa** (Reactor)
- ✅ **Outbox Pattern** (Eventos assíncronos)
- ✅ **Idempotência** (Operações críticas)
- ✅ **Transactional Operator** (R2DBC)
- ✅ **RBAC** (Role-Based Access Control)
- ✅ **Separação de camadas** (API, domínio, infra)
- ✅ **Domain-Driven Design** (DDD)


## Autor

Desenvolvido como projeto de portfólio, demonstrando conhecimento em:
- Arquitetura de sistemas reativos
- Spring WebFlux e programação reativa
- Padrões de design e arquitetura
- Boas práticas de desenvolvimento
- CI/CD e DevOps

## 🔗 Links Úteis

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [R2DBC Documentation](https://r2dbc.io/)
- [Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

