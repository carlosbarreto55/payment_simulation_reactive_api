package checkout.domain.payment.service;

import checkout.common.exception.DuplicateIdempotencyKeyException;
import checkout.domain.client.PaymentServiceClient;
import checkout.domain.payment.dto.CreateBillingRequestDto;
import checkout.domain.payment.dto.CreateBillingRequestExtDto;
import checkout.domain.payment.dto.CreateBillingResponseDto;
import checkout.domain.payment.dto.CreateBillingResponseExtDto;
import checkout.domain.payment.entity.PaymentIntent;
import checkout.domain.payment.entity.PaymentTransaction;
import checkout.domain.payment.mapper.PaymentMapper;
import checkout.domain.payment.repository.PaymentIntentRepository;
import checkout.domain.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentServiceClient paymentServiceClient;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMapper paymentMapper;

    /**
     * Creates a billing via the external payment provider.
     * <p>
     * Flow:
     * 1. Check idempotency — reject duplicates.
     * 2. Calculate total amount from product items.
     * 3. Persist a PaymentIntent with PENDING status.
     * 4. Map internal request to the provider-specific DTO and call the PSP.
     * 5. Persist a PaymentTransaction with the provider response.
     * 6. Update PaymentIntent status to PROCESSING.
     * 7. Return the internal response DTO.
     */
    @Transactional
    public Mono<CreateBillingResponseDto> createBilling(CreateBillingRequestDto request, String idempotencyKey) {
        log.info("Creating billing. idempotencyKey={}, methods={}", idempotencyKey, request.getMethodsAsStrings());

        return paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.<PaymentIntent>error(new DuplicateIdempotencyKeyException(idempotencyKey));
                    }

                    int totalAmountInCents = calculateTotalAmountInCents(request);

                    PaymentIntent intent = PaymentIntent.builder()
                            .amount(BigDecimal.valueOf(totalAmountInCents, 2))
                            .currency("BRL")
                            .status("PENDING")
                            .paymentMethod(String.join(",", request.getMethodsAsStrings()))
                            .idempotencyKey(idempotencyKey)
                            .createdAt(LocalDateTime.now())
                            .build();

                    return paymentIntentRepository.save(intent);
                })
                .flatMap(savedIntent -> {
                    CreateBillingRequestExtDto extRequest = paymentMapper.toExternalRequest(request);

                    return paymentServiceClient.createBilling(extRequest)
                            .flatMap(extResponse -> persistTransactionAndUpdateIntent(savedIntent, extResponse)
                                    .map(updatedIntent -> paymentMapper.toPaymentResponseDto(updatedIntent, extResponse)))
                            .onErrorResume(error -> handleProviderError(savedIntent, error));
                })
                .doOnSuccess(response -> log.info("Billing created successfully. paymentIntentId={}, status={}",
                        response.getPaymentIntentId(), response.getStatus()))
                .doOnError(error -> log.error("Failed to create billing. idempotencyKey={}, error={}",
                        idempotencyKey, error.getMessage()));
    }

    private int calculateTotalAmountInCents(CreateBillingRequestDto request) {
        return request.getProducts().stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    private Mono<PaymentIntent> persistTransactionAndUpdateIntent(PaymentIntent intent,
                                                                   CreateBillingResponseExtDto extResponse) {
        CreateBillingResponseExtDto.BillingDataDto data = extResponse.getData();

        String externalId = data != null ? data.getId() : null;
        String providerStatus = data != null ? data.getStatus() : "UNKNOWN";

        PaymentTransaction transaction = PaymentTransaction.builder()
                .paymentIntentId(intent.getId())
                .externalId(externalId)
                .status(providerStatus)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return paymentTransactionRepository.save(transaction)
                .flatMap(savedTx -> {
                    intent.setStatus("PROCESSING");
                    return paymentIntentRepository.save(intent);
                });
    }

    private Mono<CreateBillingResponseDto> handleProviderError(PaymentIntent intent, Throwable error) {
        log.error("Payment provider call failed. paymentIntentId={}, error={}", intent.getId(), error.getMessage());

        PaymentTransaction failedTransaction = PaymentTransaction.builder()
                .paymentIntentId(intent.getId())
                .status("DENIED")
                .failureReason(error.getMessage())
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return paymentTransactionRepository.save(failedTransaction)
                .flatMap(tx -> {
                    intent.setStatus("DENIED");
                    return paymentIntentRepository.save(intent);
                })
                .map(updatedIntent -> paymentMapper.toPaymentResponseDto(updatedIntent, null));
    }
}
