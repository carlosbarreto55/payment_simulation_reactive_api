package checkout.domain.customer.controller;

import checkout.domain.customer.service.CustomerService;
import checkout.domain.customer.dto.CustomeUpdateRequestDto;
import checkout.domain.customer.dto.CustomerRequestDto;
import checkout.domain.customer.dto.CustomerResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public Mono<ResponseEntity<CustomerResponseDto>> create(@RequestBody @Valid CustomerRequestDto body) {
        log.info("POST /api/customers - Creating customer");
        return service.create(body)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPPORT')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /api/customers - Deleting customer");
        return service.deleteCustomer(id)
                .then(Mono.fromCallable(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build()));

    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<CustomerResponseDto>> getCustomer(@PathVariable Long id) {
        log.info("GET /api/customers - Geting customer for Id {}",id);
        return service.getCustomerById(id)
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MERCHANT_ADMIN', 'SUPPORT')")
    public Mono<ResponseEntity<CustomerResponseDto>> updateCustomer(
            @RequestBody @Valid CustomeUpdateRequestDto body,
            @PathVariable Long id) {
        log.info("PATCH /api/customers - Updating customer with Id {}",id);
        return service.updateCustomer(id, body)
                .map(ResponseEntity::ok);
    }

}
