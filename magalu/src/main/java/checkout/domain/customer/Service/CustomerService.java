package checkout.domain.customer.Service;

import checkout.common.exception.ResourceNotFoundException;
import checkout.common.exception.UnauthorizedException;
import checkout.config.security.JwtAuthToken;
import checkout.domain.customer.dto.CustomeUpdateRequestDto;
import checkout.domain.customer.dto.CustomerRequestDto;
import checkout.domain.customer.dto.CustomerResponseDto;
import checkout.domain.customer.entity.Customer;
import checkout.domain.customer.entity.Document;
import checkout.domain.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Mono<CustomerResponseDto> create(CustomerRequestDto request) {
        log.debug("Creating customer with document number: {}", request.getDocumentNumber());

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> {
                    JwtAuthToken auth = (JwtAuthToken) context.getAuthentication();
                    Long userId = auth.getUserId();

                    return customerRepository.existsByDocumentNumber(request.getDocumentNumber())
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    log.error("Document number already exists: {}", request.getDocumentNumber());
                                    return Mono.error(new IllegalArgumentException("Document number already exists"));
                                }
                                Customer customer = buildCustomerFromRequest(request);
                                customer.setUserId(userId);

                                return customerRepository.save(customer)
                                        .map(this::buildResponseDto);
                            });
                });
    }

    public Mono<CustomerResponseDto> getCustomerById(Long id) {
        log.debug("Getting customer by ID: {}", id);

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    JwtAuthToken auth = (JwtAuthToken) securityContext.getAuthentication();
                    Long userId = auth.getUserId();
                    List<String> roles = auth.getRoles();

                    return customerRepository.findById(id)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer not found with id: " + id)))
                            .flatMap(customer -> {
                                if (!isAuthorized(roles, userId, customer.getUserId())) {
                                    return Mono.error(new UnauthorizedException(
                                            "You are not authorized to access this customer"));
                                }
                                return Mono.just(this.buildResponseDto(customer));
                            });
                });
    }

    @Transactional
    public Mono<CustomerResponseDto> updateCustomer(Long id, CustomeUpdateRequestDto request) {
        log.info("Updating customer with id: {}", id);

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    JwtAuthToken auth = (JwtAuthToken) securityContext.getAuthentication();
                    Long userId = auth.getUserId();
                    List<String> roles = auth.getRoles();

                    return customerRepository.findById(id)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer not found with id: " + id)))
                            .flatMap(customer -> {
                                if (!isAuthorized(roles, userId, customer.getUserId())) {
                                    return Mono.error(new UnauthorizedException(
                                            "You are not authorized to update this customer"));
                                }
                                updateCustomerFields(customer, request);
                                return customerRepository.save(customer)
                                        .map(this::buildResponseDto);
                            });
                });
    }

    @Transactional
    public Mono<Void> deleteCustomer(Long customerId) {
        log.info("Deleting customer with id: {}", customerId);

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    JwtAuthToken auth = (JwtAuthToken) securityContext.getAuthentication();
                    Long userId = auth.getUserId();
                    List<String> roles = auth.getRoles();

                    return customerRepository.findById(customerId)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer not found with id: " + customerId)))
                            .flatMap(customer -> {
                                if (!isAuthorized(roles, userId, customer.getUserId())) {
                                    return Mono.error(new UnauthorizedException(
                                            "You are not authorized to delete this customer"));
                                }
                                return customerRepository.deleteById(customerId);
                            })
                            .then();
                });
    }
    private Boolean isAuthorized(List<String> roles, Long authUserId, Long customerUserId) {
        return roles.contains("SUPPORT") || authUserId.equals(customerUserId);
    }

    private void updateCustomerFields(Customer customer, CustomeUpdateRequestDto request) {
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
    }

    private Customer buildCustomerFromRequest(CustomerRequestDto request) {
        return Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .document(extractDocumentFromRequest(request))
                .build();
    }

    private Document extractDocumentFromRequest(CustomerRequestDto request) {
        return Document.builder()
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .build();
    }

    private CustomerResponseDto buildResponseDto(Customer customer) {
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .build();
    }
}