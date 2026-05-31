package checkout.boundedcontext.payment.domain;

import checkout.boundedcontext.payment.domain.port.CreateBillingDomainRequest;
import checkout.boundedcontext.payment.domain.port.ExternalBillingResponse;
import checkout.boundedcontext.payment.domain.port.PaymentProviderPort;
import checkout.boundedcontext.payment.domain.repository.PaymentIntentRepository;
import checkout.common.domain.event.DomainEvent;
import checkout.common.domain.event.EventBus;
import checkout.common.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PaymentDomainService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentProviderPort paymentProviderPort;
    private final EventBus eventBus;

    public Mono<PaymentIntent> initiatePayment(CreateBillingDomainRequest request, IdempotencyKey idempotencyKey) {
        log.info("Initiating payment. idempotencyKey={}, methods={}", idempotencyKey.value(), request.methods());

        return paymentIntentRepository.existsByIdempotencyKey(idempotencyKey)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return paymentIntentRepository.findByIdempotencyKey(idempotencyKey);
                    }
                    PaymentIntent intent = buildPaymentInitiation(request, idempotencyKey);
                    return paymentIntentRepository.save(intent);
                })
                .flatMap(this::publishEventsAndClear)
                .flatMap(savedIntent -> {
                    log.info("PaymentIntent persisted. id={}, calling PSP", savedIntent.getId());
                    return paymentProviderPort.createBilling(request)
                            .flatMap(response -> processAndSavePaymentIntent(savedIntent, response))
                            .onErrorResume(error -> {
                                log.error("PSP call failed. paymentIntentId={}, error={}",
                                        savedIntent.getId(), error.getMessage());
                                savedIntent.deny("PSP processing error");
                                return paymentIntentRepository.save(savedIntent)
                                        .flatMap(this::publishEventsAndClear);
                            });
                });
    }

    private Mono<PaymentIntent> processAndSavePaymentIntent(PaymentIntent savedIntent, ExternalBillingResponse response) {
        savedIntent.process(
                ExternalBillingId.of(response.billingId()),
                response.status()
        );
        return paymentIntentRepository.save(savedIntent)
                .flatMap(this::publishEventsAndClear);
    }

    private PaymentIntent buildPaymentInitiation(CreateBillingDomainRequest request, IdempotencyKey idempotencyKey) {
        Money totalAmount = calculateTotalFromProducts(request.products());
        return PaymentIntent.initiate(
                idempotencyKey, totalAmount, request.methods(),
                null,
                request.products()
        );
    }

    public Mono<PaymentIntent> getPaymentIntent(PaymentIntentId id) {
        return paymentIntentRepository.findById(id);
    }

    public Mono<PaymentIntent> getPaymentIntentWithTransactions(PaymentIntentId id) {
        return paymentIntentRepository.findByIdWithTransactions(id);
    }

    private Mono<PaymentIntent> publishEventsAndClear(PaymentIntent intent) {
        List<DomainEvent> events = intent.getDomainEvents();
        if (events.isEmpty()) {
            return Mono.just(intent);
        }
        return Flux.fromIterable(events)
                .concatMap(eventBus::publish)
                .then(Mono.fromRunnable(intent::clearDomainEvents))
                .thenReturn(intent);
    }

    private Money calculateTotalFromProducts(List<ProductItem> products) {
        long totalCents = products.stream()
                .mapToLong(p -> (long) p.priceInCents() * (long) p.quantity())
                .sum();
        if (totalCents > Integer.MAX_VALUE) {
            throw new ArithmeticException("Total amount exceeds maximum allowed value");
        }
        return Money.ofCents((int) totalCents);
    }
}
