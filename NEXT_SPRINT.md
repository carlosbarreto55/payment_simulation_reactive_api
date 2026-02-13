# 🚀 Next Sprint - Prioridades de Implementação

**Projeto:** Magalu Checkout & Payment Platform  
**Data:** Fevereiro 2026  
**Progresso Atual:** ~35% (Auth + Customers + Infraestrutura)

---

## 📊 Status do Projeto

### ✅ Já Implementado

| Módulo | Status | Completude |
|--------|--------|------------|
| **Auth Module** | ✅ Completo | 95% |
| - User/Role Entities | ✅ | 100% |
| - JWT Service | ✅ | 100% |
| - Auth Service (Register/Login/Refresh/Logout) | ✅ | 100% |
| - Auth Controller (4 endpoints) | ✅ | 100% |
| - JWT Filter (JwtAuthWebFilter) | ✅ | 100% |
| - Security Config | ⚠️ Parcial | 50% |
| **Customer Module** | ⚠️ Parcial | 70% |
| - Customer Entity | ✅ | 100% |
| - Customer Service (CRUD) | ✅ | 100% |
| - Customer Controller | ⚠️ Incompleto | 40% |
| **Infraestrutura** | ✅ Completo | 100% |
| - Migrations (8 arquivos) | ✅ | 100% |
| - Exception System (21 classes) | ✅ | 100% |
| - R2DBC Config | ✅ | 100% |
| **Order Module** | ❌ | 0% |
| **Payment Module** | ❌ | 0% |
| **Risk Module** | ❌ | 0% |
| **Notification Module** | ❌ | 0% |
| **Webhook Module** | ❌ | 0% |
| **Idempotency Service** | ❌ | 0% |
| **Audit Service** | ❌ | 0% |

---

## 🎯 Prioridade 1: Security Layer Completion (CRÍTICO)

**Estimativa:** 2-3 horas  
**Dependências:** Nenhuma  
**Bloqueia:** Teste de endpoints protegidos

### Contexto

O sistema **já possui** o `JwtAuthWebFilter` implementado, mas o `SecurityConfig` está configurado com `permitAll()`, deixando todos os endpoints públicos. Além disso, o `CustomerController` está incompleto (sem `@RequestMapping` e construtor).

### Objetivos

- [x] Integrar `JwtAuthWebFilter` no `SecurityConfig`
- [x] Proteger endpoints da API (exceto `/api/auth/**`)
- [x] Corrigir bugs no `CustomerController`
- [x] Adicionar Role-Based Access Control (RBAC)
- [x] Testar fluxo completo de autenticação

---

### 📝 Task 1.1: Atualizar SecurityConfig

**Arquivo:** `checkout/config/SecurityConfig.java`

**Problema Atual:**
```java
.authorizeExchange(exchanges -> exchanges
    .anyExchange().permitAll()
)
```

**Implementação:**

```java
package checkout.config;

import checkout.config.security.JwtAuthWebFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthWebFilter jwtAuthWebFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Endpoints públicos
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/actuator/prometheus").permitAll()
                        
                        // Todos os outros endpoints requerem autenticação
                        .anyExchange().authenticated()
                )
                // Adicionar JWT Filter na cadeia
                .addFilterAt(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Mudanças:**
1. ✅ Injeção do `JwtAuthWebFilter` via construtor
2. ✅ Endpoints `/api/auth/**` e `/actuator/**` públicos
3. ✅ Todos os outros endpoints requerem autenticação
4. ✅ Filtro JWT adicionado na ordem correta (`AUTHENTICATION`)
5. ✅ `@EnableReactiveMethodSecurity` para suportar anotações `@PreAuthorize`

---

### 📝 Task 1.2: Corrigir JwtAuthWebFilter (Melhorias)

**Arquivo:** `checkout/config/security/JwtAuthWebFilter.java`

**Problemas Encontrados:**
1. ⚠️ Log typo: "An error ocuured"
2. ⚠️ Método `isPublicPath()` definido mas não usado
3. ⚠️ Typo no path: `/api/auth/registe` (faltando 'r')

**Implementação Corrigida:**

```java
package checkout.config.security;

import checkout.common.HeaderProcessor;
import checkout.domain.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthWebFilter implements WebFilter {

    private final JwtService jwtService;
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Se for endpoint público, pular validação JWT
        if (isPublicPath(path)) {
            log.debug("Public path accessed: {}", path);
            return chain.filter(exchange);
        }

        String token = HeaderProcessor.extractToken(exchange);

        if (token != null) {
            return jwtService.validateAccessToken(token)
                    .flatMap(claims -> {
                        JwtAuthToken authentication = createAuthentication(claims, token);
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                    })
                    .onErrorResume(e -> {
                        log.warn("An error occurred validating token: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
        } else {
            log.debug("No token found in request for path: {}", path);
            return chain.filter(exchange);
        }
    }

    private JwtAuthToken createAuthentication(Claims claims, String token) {
        Long userId = claims.get("userId", Long.class);
        String email = claims.get("email", String.class);
        List<String> roles = claims.get("roles", List.class);

        log.debug("Authentication created for user: {} with roles: {}", email, roles);
        return new JwtAuthToken(userId, email, roles, token);
    }

    private Boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/actuator/");
    }
}
```

**Melhorias:**
1. ✅ Usa `isPublicPath()` para evitar validação JWT em rotas públicas
2. ✅ Correção de typos
3. ✅ Logs mais descritivos

---

### 📝 Task 1.3: Corrigir CustomerController

**Arquivo:** `checkout/domain/customer/controller/CustomerController.java`

**Problemas Atuais:**
1. ❌ Falta `@RequestMapping("/api/customers")`
2. ❌ Falta construtor para injetar `CustomerService`
3. ❌ Falta `@RequiredArgsConstructor`
4. ❌ Falta endpoints GET, PUT, DELETE
5. ❌ Usa `@Controller` em vez de `@RestController`

**Implementação Completa:**

```java
package checkout.domain.customer.controller;

import checkout.domain.customer.Service.CustomerService;
import checkout.domain.customer.dto.CustomeUpdateRequestDto;
import checkout.domain.customer.dto.CustomerRequestDto;
import checkout.domain.customer.dto.CustomerResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Criar novo cliente
     * Requer autenticação: Qualquer usuário autenticado pode criar um cliente
     */
    @PostMapping
    public Mono<ResponseEntity<CustomerResponseDto>> create(@RequestBody @Valid CustomerRequestDto request) {
        log.info("POST /api/customers - Creating customer");
        return customerService.create(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Buscar cliente por ID
     * Requer role: CUSTOMER, MERCHANT_ADMIN, SUPPORT
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<CustomerResponseDto>> getById(@PathVariable Long id) {
        log.info("GET /api/customers/{} - Getting customer by ID", id);
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok);
    }

    /**
     * Buscar cliente por número de documento
     * Requer role: MERCHANT_ADMIN, SUPPORT
     */
    @GetMapping("/document/{documentNumber}")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<CustomerResponseDto>> getByDocument(@PathVariable String documentNumber) {
        log.info("GET /api/customers/document/{} - Getting customer by document", documentNumber);
        return customerService.findByDocumentNumber(documentNumber)
                .map(ResponseEntity::ok);
    }

    /**
     * Atualizar cliente
     * Requer role: CUSTOMER (próprio), MERCHANT_ADMIN, SUPPORT
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<CustomerResponseDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid CustomeUpdateRequestDto request) {
        log.info("PUT /api/customers/{} - Updating customer", id);
        return customerService.updateCustomer(id, request)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletar cliente
     * Requer role: SUPPORT (apenas suporte pode deletar)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPPORT')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /api/customers/{} - Deleting customer", id);
        return customerService.deleteCustomer(id)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
```

**Mudanças:**
1. ✅ `@RestController` em vez de `@Controller`
2. ✅ `@RequestMapping("/api/customers")` adicionado
3. ✅ `@RequiredArgsConstructor` para injeção de dependência
4. ✅ 5 endpoints completos (POST, GET by ID, GET by Document, PUT, DELETE)
5. ✅ RBAC com `@PreAuthorize` em endpoints sensíveis
6. ✅ Documentação via JavaDoc

---

### 📝 Task 1.4: Testes de Autenticação

**Criar arquivo:** `test-auth-flow.http` (na raiz do projeto)

```http
### 1. Registrar novo usuário
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "test@magalu.com",
  "password": "Test@1234"
}

### 2. Login
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "test@magalu.com",
  "password": "Test@1234"
}

### 3. Criar Customer (com token)
POST http://localhost:8080/api/customers
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "name": "João Silva",
  "email": "joao@example.com",
  "documentType": "CPF",
  "documentNumber": "12345678901",
  "phoneNumber": "11987654321"
}

### 4. Tentar acessar sem token (deve falhar)
GET http://localhost:8080/api/customers/1

### 5. Acessar com token
GET http://localhost:8080/api/customers/1
Authorization: Bearer {{accessToken}}

### 6. Refresh Token
POST http://localhost:8080/api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "{{refreshToken}}"
}

### 7. Logout
POST http://localhost:8080/api/auth/logout
Content-Type: application/json

{
  "refreshToken": "{{refreshToken}}"
}
```

---

### ✅ Checklist Prioridade 1

- [ ] Atualizar `SecurityConfig.java` com JWT Filter
- [ ] Corrigir `JwtAuthWebFilter.java` (typos e lógica)
- [ ] Corrigir `CustomerController.java` (endpoints completos + RBAC)
- [ ] Criar arquivo `test-auth-flow.http`
- [ ] Testar registro de usuário
- [ ] Testar login e obtenção de tokens
- [ ] Testar acesso a endpoint protegido **sem** token (deve retornar 401)
- [ ] Testar acesso a endpoint protegido **com** token válido (deve funcionar)
- [ ] Testar refresh token
- [ ] Testar logout
- [ ] Verificar RBAC funcionando (ex: DELETE /customers só com role SUPPORT)

---

## 🎯 Prioridade 2: Order Service Implementation (ALTO)

**Estimativa:** 5-7 horas  
**Dependências:** Customer Service  
**Bloqueia:** Payment Service

### Contexto

O módulo de **Orders** é o coração do sistema de checkout. Ele gerencia pedidos, itens, estados e regras de negócio complexas. A tabela `orders` e `order_items` **já existem** no banco via migration V3.

### Objetivos

- [x] Criar entidades (Order, OrderItem)
- [x] Criar enums (OrderStatus)
- [x] Criar DTOs (Request/Response)
- [x] Criar repositories
- [x] Implementar service com regras de negócio
- [x] Criar controller com 3 endpoints principais
- [x] Documentar regras de negócio

---

### 📝 Task 2.1: Criar Enums

**Criar arquivo:** `checkout/common/enums/OrderStatus.java`

```java
package checkout.common.enums;

public enum OrderStatus {
    CREATED("Pedido criado, aguardando pagamento"),
    RESERVED("Estoque reservado, aguardando pagamento"),
    PAID("Pedido pago com sucesso"),
    CANCELED("Pedido cancelado"),
    REFUNDED("Pedido estornado");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Verifica se o pedido pode ser cancelado
     */
    public boolean canBeCanceled() {
        return this == CREATED || this == RESERVED;
    }

    /**
     * Verifica se o pedido pode ser pago
     */
    public boolean canBePaid() {
        return this == CREATED || this == RESERVED;
    }

    /**
     * Verifica se o pedido pode ser estornado
     */
    public boolean canBeRefunded() {
        return this == PAID;
    }
}
```

---

### 📝 Task 2.2: Criar Entidades

**Criar arquivo:** `checkout/domain/order/entity/Order.java`

```java
package checkout.domain.order.entity;

import checkout.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private Long id;

    @Column("customer_id")
    private Long customerId;

    @Column("status")
    private OrderStatus status;

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Lógica de negócio: verificar se pode ser cancelado
     */
    public boolean canBeCanceled() {
        return status != null && status.canBeCanceled();
    }

    /**
     * Lógica de negócio: verificar se pode ser pago
     */
    public boolean canBePaid() {
        return status != null && status.canBePaid();
    }

    /**
     * Lógica de negócio: verificar se pode ser estornado
     */
    public boolean canBeRefunded() {
        return status != null && status.canBeRefunded();
    }
}
```

**Criar arquivo:** `checkout/domain/order/entity/OrderItem.java`

```java
package checkout.domain.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("product_id")
    private String productId;

    @Column("product_name")
    private String productName;

    @Column("quantity")
    private Integer quantity;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("total_price")
    private BigDecimal totalPrice;

    /**
     * Calcula o total baseado em quantidade e preço unitário
     */
    public void calculateTotal() {
        if (quantity != null && unitPrice != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
```

---

### 📝 Task 2.3: Criar DTOs

**Criar arquivo:** `checkout/domain/order/dto/OrderItemDto.java`

```java
package checkout.domain.order.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;
}
```

**Criar arquivo:** `checkout/domain/order/dto/OrderRequestDto.java`

```java
package checkout.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemDto> items;
}
```

**Criar arquivo:** `checkout/domain/order/dto/OrderResponseDto.java`

```java
package checkout.domain.order.dto;

import checkout.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private Long id;
    private Long customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### 📝 Task 2.4: Criar Repositories

**Criar arquivo:** `checkout/domain/order/repository/OrderRepository.java`

```java
package checkout.domain.order.repository;

import checkout.common.enums.OrderStatus;
import checkout.domain.order.entity.Order;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    /**
     * Buscar pedidos de um cliente
     */
    Flux<Order> findByCustomerId(Long customerId);

    /**
     * Buscar pedidos de um cliente com status específico
     */
    Flux<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    /**
     * Contar pedidos de um cliente
     */
    Mono<Long> countByCustomerId(Long customerId);

    /**
     * Buscar pedidos criados após uma data
     */
    @Query("SELECT * FROM orders WHERE created_at >= :startDate ORDER BY created_at DESC")
    Flux<Order> findRecentOrders(String startDate);
}
```

**Criar arquivo:** `checkout/domain/order/repository/OrderItemRepository.java`

```java
package checkout.domain.order.repository;

import checkout.domain.order.entity.OrderItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, Long> {

    /**
     * Buscar itens de um pedido
     */
    Flux<OrderItem> findByOrderId(Long orderId);

    /**
     * Deletar itens de um pedido (usado ao cancelar)
     */
    Mono<Void> deleteByOrderId(Long orderId);

    /**
     * Contar itens de um pedido
     */
    Mono<Long> countByOrderId(Long orderId);
}
```

---

### 📝 Task 2.5: Criar Service

**Criar arquivo:** `checkout/domain/order/service/OrderService.java`

```java
package checkout.domain.order.service;

import checkout.common.enums.OrderStatus;
import checkout.common.exception.*;
import checkout.domain.customer.repository.CustomerRepository;
import checkout.domain.order.dto.OrderItemDto;
import checkout.domain.order.dto.OrderRequestDto;
import checkout.domain.order.dto.OrderResponseDto;
import checkout.domain.order.entity.Order;
import checkout.domain.order.entity.OrderItem;
import checkout.domain.order.repository.OrderItemRepository;
import checkout.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;

    /**
     * Criar um novo pedido
     * 
     * Regras de negócio:
     * 1. Cliente deve existir
     * 2. Pedido deve ter pelo menos 1 item
     * 3. Total é calculado automaticamente
     * 4. Status inicial: CREATED
     * 5. Simula reserva de estoque (sempre sucesso)
     */
    @Transactional
    public Mono<OrderResponseDto> createOrder(OrderRequestDto request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        // Validar se cliente existe
        return customerRepository.existsById(request.getCustomerId())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException(
                                "Customer not found with ID: " + request.getCustomerId()));
                    }

                    // Calcular total do pedido
                    BigDecimal totalAmount = calculateTotal(request.getItems());

                    // Criar ordem
                    Order order = Order.builder()
                            .customerId(request.getCustomerId())
                            .status(OrderStatus.CREATED)
                            .totalAmount(totalAmount)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return orderRepository.save(order)
                            .flatMap(savedOrder -> saveOrderItems(savedOrder.getId(), request.getItems())
                                    .then(simulateStockReservation(savedOrder))
                                    .flatMap(reservedOrder -> buildOrderResponse(reservedOrder)));
                });
    }

    /**
     * Buscar pedido por ID
     */
    public Mono<OrderResponseDto> getOrderById(Long orderId) {
        log.debug("Getting order by ID: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with ID: " + orderId)))
                .flatMap(this::buildOrderResponse);
    }

    /**
     * Buscar pedidos de um cliente
     */
    public Flux<OrderResponseDto> getOrdersByCustomerId(Long customerId) {
        log.debug("Getting orders for customer: {}", customerId);

        return orderRepository.findByCustomerId(customerId)
                .flatMap(this::buildOrderResponse);
    }

    /**
     * Cancelar pedido
     * 
     * Regras de negócio:
     * 1. Apenas pedidos CREATED ou RESERVED podem ser cancelados
     * 2. Pedidos PAID devem ser estornados, não cancelados
     */
    @Transactional
    public Mono<OrderResponseDto> cancelOrder(Long orderId) {
        log.info("Canceling order: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with ID: " + orderId)))
                .flatMap(order -> {
                    // Verificar se pode ser cancelado
                    if (!order.canBeCanceled()) {
                        return Mono.error(new OrderCancellationNotAllowedException(
                                String.format("Order with status %s cannot be canceled. Current status: %s",
                                        order.getStatus(), order.getStatus().getDescription())));
                    }

                    // Atualizar status
                    order.setStatus(OrderStatus.CANCELED);
                    order.setUpdatedAt(LocalDateTime.now());

                    return orderRepository.save(order)
                            .flatMap(this::buildOrderResponse);
                });
    }

    /**
     * Atualizar status do pedido para PAID
     * (Será chamado pelo Payment Service)
     */
    @Transactional
    public Mono<Order> markOrderAsPaid(Long orderId) {
        log.info("Marking order as PAID: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with ID: " + orderId)))
                .flatMap(order -> {
                    if (!order.canBePaid()) {
                        return Mono.error(new InvalidOrderStatusException(
                                String.format("Order cannot be paid. Current status: %s", order.getStatus())));
                    }

                    order.setStatus(OrderStatus.PAID);
                    order.setUpdatedAt(LocalDateTime.now());

                    return orderRepository.save(order);
                });
    }

    /**
     * Atualizar status do pedido para REFUNDED
     * (Será chamado pelo Payment Service)
     */
    @Transactional
    public Mono<Order> markOrderAsRefunded(Long orderId) {
        log.info("Marking order as REFUNDED: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with ID: " + orderId)))
                .flatMap(order -> {
                    if (!order.canBeRefunded()) {
                        return Mono.error(new RefundNotAllowedException(
                                String.format("Order cannot be refunded. Current status: %s", order.getStatus())));
                    }

                    order.setStatus(OrderStatus.REFUNDED);
                    order.setUpdatedAt(LocalDateTime.now());

                    return orderRepository.save(order);
                });
    }

    // ========== MÉTODOS AUXILIARES ==========

    /**
     * Calcular total do pedido
     */
    private BigDecimal calculateTotal(List<OrderItemDto> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Salvar itens do pedido
     */
    private Mono<Void> saveOrderItems(Long orderId, List<OrderItemDto> items) {
        return Flux.fromIterable(items)
                .map(itemDto -> {
                    OrderItem item = OrderItem.builder()
                            .orderId(orderId)
                            .productId(itemDto.getProductId())
                            .productName(itemDto.getProductName())
                            .quantity(itemDto.getQuantity())
                            .unitPrice(itemDto.getUnitPrice())
                            .build();
                    item.calculateTotal();
                    return item;
                })
                .flatMap(orderItemRepository::save)
                .then();
    }

    /**
     * Simular reserva de estoque
     * (Em produção, integraria com serviço de inventory)
     */
    private Mono<Order> simulateStockReservation(Order order) {
        log.debug("Simulating stock reservation for order: {}", order.getId());

        // Simulação: sempre sucesso
        // Em produção: chamaria API de estoque
        boolean reservationSuccess = true;

        if (reservationSuccess) {
            order.setStatus(OrderStatus.RESERVED);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        } else {
            // Se falhar, cancelar pedido
            return Mono.error(new StockReservationFailedException(
                    "Failed to reserve stock for order: " + order.getId()));
        }
    }

    /**
     * Construir DTO de resposta com itens
     */
    private Mono<OrderResponseDto> buildOrderResponse(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .map(item -> OrderItemDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collectList()
                .map(items -> OrderResponseDto.builder()
                        .id(order.getId())
                        .customerId(order.getCustomerId())
                        .status(order.getStatus())
                        .totalAmount(order.getTotalAmount())
                        .items(items)
                        .createdAt(order.getCreatedAt())
                        .updatedAt(order.getUpdatedAt())
                        .build());
    }
}
```

---

### 📝 Task 2.6: Criar Controller

**Criar arquivo:** `checkout/domain/order/controller/OrderController.java`

```java
package checkout.domain.order.controller;

import checkout.domain.order.dto.OrderRequestDto;
import checkout.domain.order.dto.OrderResponseDto;
import checkout.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Criar novo pedido
     * Requer autenticação
     */
    @PostMapping
    public Mono<ResponseEntity<OrderResponseDto>> createOrder(@RequestBody @Valid OrderRequestDto request) {
        log.info("POST /api/orders - Creating order for customer: {}", request.getCustomerId());
        return orderService.createOrder(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Buscar pedido por ID
     * Requer autenticação
     * TODO: Adicionar validação de ownership (usuário só vê seus próprios pedidos)
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<OrderResponseDto>> getOrderById(@PathVariable Long orderId) {
        log.info("GET /api/orders/{} - Getting order", orderId);
        return orderService.getOrderById(orderId)
                .map(ResponseEntity::ok);
    }

    /**
     * Buscar pedidos de um cliente
     * Requer role: MERCHANT_ADMIN, SUPPORT
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<Flux<OrderResponseDto>>> getOrdersByCustomer(@PathVariable Long customerId) {
        log.info("GET /api/orders/customer/{} - Getting customer orders", customerId);
        Flux<OrderResponseDto> orders = orderService.getOrdersByCustomerId(customerId);
        return Mono.just(ResponseEntity.ok(orders));
    }

    /**
     * Cancelar pedido
     * Requer role: CUSTOMER (próprio), MERCHANT_ADMIN, SUPPORT
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<OrderResponseDto>> cancelOrder(@PathVariable Long orderId) {
        log.info("POST /api/orders/{}/cancel - Canceling order", orderId);
        return orderService.cancelOrder(orderId)
                .map(ResponseEntity::ok);
    }
}
```

---

### 📝 Task 2.7: Testes de Order Service

**Criar arquivo:** `test-order-flow.http` (na raiz do projeto)

```http
### Setup: Obter token
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "test@magalu.com",
  "password": "Test@1234"
}

### 1. Criar Customer
POST http://localhost:8080/api/customers
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "name": "João Silva",
  "email": "joao@example.com",
  "documentType": "CPF",
  "documentNumber": "12345678901",
  "phoneNumber": "11987654321"
}

### 2. Criar Pedido (customerId obtido do passo anterior)
POST http://localhost:8080/api/orders
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "customerId": 1,
  "items": [
    {
      "productId": "PROD001",
      "productName": "iPhone 15 Pro",
      "quantity": 1,
      "unitPrice": 7999.00
    },
    {
      "productId": "PROD002",
      "productName": "AirPods Pro",
      "quantity": 2,
      "unitPrice": 1999.00
    }
  ]
}

### 3. Buscar Pedido por ID
GET http://localhost:8080/api/orders/1
Authorization: Bearer {{accessToken}}

### 4. Buscar Pedidos de um Cliente
GET http://localhost:8080/api/orders/customer/1
Authorization: Bearer {{accessToken}}

### 5. Cancelar Pedido
POST http://localhost:8080/api/orders/1/cancel
Authorization: Bearer {{accessToken}}

### 6. Tentar cancelar pedido já cancelado (deve falhar)
POST http://localhost:8080/api/orders/1/cancel
Authorization: Bearer {{accessToken}}

### 7. Criar pedido com cliente inexistente (deve falhar)
POST http://localhost:8080/api/orders
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "customerId": 99999,
  "items": [
    {
      "productId": "PROD001",
      "productName": "Produto Teste",
      "quantity": 1,
      "unitPrice": 100.00
    }
  ]
}

### 8. Criar pedido sem itens (deve falhar - validação)
POST http://localhost:8080/api/orders
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "customerId": 1,
  "items": []
}
```

---

### ✅ Checklist Prioridade 2

- [ ] Criar `OrderStatus` enum
- [ ] Criar entidades `Order` e `OrderItem`
- [ ] Criar DTOs (`OrderItemDto`, `OrderRequestDto`, `OrderResponseDto`)
- [ ] Criar repositories (`OrderRepository`, `OrderItemRepository`)
- [ ] Implementar `OrderService` com todas as regras de negócio
- [ ] Criar `OrderController` com 4 endpoints
- [ ] Criar arquivo `test-order-flow.http`
- [ ] Testar criação de pedido
- [ ] Testar busca de pedido por ID
- [ ] Testar busca de pedidos por cliente
- [ ] Testar cancelamento de pedido
- [ ] Testar regras de negócio:
  - [ ] Pedido só pode ser cancelado se CREATED ou RESERVED
  - [ ] Total calculado automaticamente
  - [ ] Stock reservation (simulado) funciona
  - [ ] Validações de DTOs funcionam
- [ ] Verificar logs estruturados

---

## 📈 Métricas de Sucesso

### Prioridade 1 (Security Layer)
- ✅ Endpoints `/api/auth/**` acessíveis sem token
- ✅ Endpoints `/api/customers/**` requerem token válido
- ✅ Requisições sem token retornam **401 Unauthorized**
- ✅ Requisições com token inválido/expirado retornam **401**
- ✅ RBAC funcionando (ex: DELETE customers só com SUPPORT)
- ✅ Refresh token funciona
- ✅ Logout revoga tokens

### Prioridade 2 (Order Service)
- ✅ Criar pedido com sucesso (retorna 201 Created)
- ✅ Total calculado automaticamente e corretamente
- ✅ Pedido inicia com status CREATED e muda para RESERVED
- ✅ Buscar pedido por ID retorna todos os dados (incluindo items)
- ✅ Cancelar pedido CREATED/RESERVED funciona
- ✅ Tentar cancelar pedido PAID retorna **400 Bad Request**
- ✅ Tentar criar pedido com cliente inexistente retorna **404**
- ✅ Validações de DTO funcionam (items vazio, quantidade negativa, etc)

---

## 🔄 Próximas Sprints (Planejamento Futuro)

### Sprint 3: Payment Service (8-10h)
- Payment Intent creation
- Payment processors (PIX, Credit Card, Debit, Boleto)
- Idempotency-Key validation
- Payment confirmation and refund
- Integration com Order Service

### Sprint 4: Risk Analysis Service (4-6h)
- Risk engine com regras
- Score calculation
- Manual approval workflow (SUPPORT)
- Integration com Payment Service

### Sprint 5: Idempotency Service (3-4h)
- IdempotencyService implementation
- IdempotencyWebFilter
- Job de limpeza automática

### Sprint 6: Audit Service (2-3h)
- AuditService implementation
- @Auditable annotation com AOP
- Auditoria em pontos críticos

---

## 📚 Recursos e Referências

### Spring WebFlux & Reactive
- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor Core](https://projectreactor.io/docs/core/release/reference/)
- [R2DBC MySQL Driver](https://github.com/asyncer-io/r2dbc-mysql)

### Spring Security
- [Spring Security WebFlux](https://docs.spring.io/spring-security/reference/reactive/index.html)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)

### Database
- [Flyway Migrations](https://flywaydb.org/documentation/)
- [R2DBC Documentation](https://r2dbc.io/)

### Testing
- [Reactor Test](https://projectreactor.io/docs/test/release/reference/)
- [Testcontainers](https://www.testcontainers.org/)

---

## 🎯 Observações Finais

### Pontos Fortes do Projeto Atual
1. ✅ Arquitetura reativa bem estruturada
2. ✅ Sistema de exceptions robusto e personalizado
3. ✅ Migrations completas para todos os módulos
4. ✅ Auth Service completo e funcional
5. ✅ JWT Filter já implementado

### Pontos de Atenção
1. ⚠️ CustomerController estava incompleto (será corrigido na Prioridade 1)
2. ⚠️ SecurityConfig não estava usando o JWT Filter (será corrigido)
3. ⚠️ Falta implementar ownership validation (usuário só acessa seus dados)
4. ⚠️ Falta implementar rate limiting
5. ⚠️ Falta implementar testes automatizados

### Recomendações
- Após completar Prioridade 1 e 2, adicionar testes unitários
- Implementar ownership validation no Customer e Order Service
- Considerar adicionar cache com Redis no futuro
- Adicionar rate limiting no Security Config
- Implementar observabilidade (logs estruturados + métricas)

---

**Data de Criação:** 09/02/2026  
**Última Atualização:** 09/02/2026  
**Próxima Revisão:** Após completar Prioridade 1
