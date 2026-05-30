package checkout.domain.customer.service;

import checkout.common.exception.DuplicateDocumentException;
import checkout.common.exception.ResourceNotFoundException;
import checkout.common.exception.UnauthorizedException;
import checkout.config.security.JwtAuthToken;
import checkout.domain.customer.dto.CustomerUpdateRequestDto;
import checkout.domain.customer.dto.CustomerRequestDto;
import checkout.domain.customer.dto.CustomerResponseDto;
import checkout.domain.customer.entity.Customer;
import checkout.boundedcontext.customer.domain.Document;
import checkout.common.enums.DocumentType;
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
        log.debug("Creating customer with document type: {}", request.getDocumentType());
        Document document = extractDocumentFromRequest(request);
        String normalizedDocumentNumber = document.documentNumber().replaceAll("\\D", "");

        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (JwtAuthToken) context.getAuthentication())
                .flatMap(auth -> customerRepository.existsByDocumentNumberAndDocumentType(normalizedDocumentNumber, document.documentType())
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                log.error("Document number already exists for type: {}", document.documentType());
                                return Mono.error(new DuplicateDocumentException());
                            }
                            Customer customer = buildCustomerFromRequest(normalizedDocumentNumber, document.documentType(), request);
                            customer.setUserId(auth.getUserId());
                            return customerRepository.save(customer)
                                    .map(this::buildResponseDto);
                        }));
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
    public Mono<CustomerResponseDto> updateCustomer(Long id, CustomerUpdateRequestDto request) {
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

    private void updateCustomerFields(Customer customer, CustomerUpdateRequestDto request) {
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
    }

    private Customer buildCustomerFromRequest(String documentNumber, DocumentType documentType, CustomerRequestDto request) {
        return Customer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .documentNumber(documentNumber)
                .documentType(documentType)
                .build();
    }

    private Document extractDocumentFromRequest(CustomerRequestDto request) {
        return Document.from(request.getDocumentType(), request.getDocumentNumber());
    }

    private CustomerResponseDto buildResponseDto(Customer customer) {
        Document document = new Document(customer.getDocumentType(), customer.getDocumentNumber());
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .document(document.toString())
                .documentType(customer.getDocumentType())
                .phoneNumber(customer.getPhoneNumber())
                .build();
    }
}