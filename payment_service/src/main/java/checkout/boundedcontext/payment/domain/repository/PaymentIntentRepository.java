package checkout.boundedcontext.payment.domain.repository;

import checkout.boundedcontext.payment.domain.IdempotencyKey;
import checkout.boundedcontext.payment.domain.PaymentIntent;
import checkout.boundedcontext.payment.domain.PaymentIntentId;
import reactor.core.publisher.Mono;

public interface PaymentIntentRepository {
    Mono<PaymentIntent> save(PaymentIntent intent);
    Mono<PaymentIntent> findById(PaymentIntentId id);
    Mono<PaymentIntent> findByIdWithTransactions(PaymentIntentId id);
    Mono<Boolean> existsByIdempotencyKey(IdempotencyKey key);
    Mono<PaymentIntent> findByIdempotencyKey(IdempotencyKey key);
}
