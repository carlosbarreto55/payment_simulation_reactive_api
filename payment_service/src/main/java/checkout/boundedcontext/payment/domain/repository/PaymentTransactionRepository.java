package checkout.boundedcontext.payment.domain.repository;

import checkout.boundedcontext.payment.domain.ExternalBillingId;
import checkout.boundedcontext.payment.domain.PaymentIntentId;
import checkout.boundedcontext.payment.domain.PaymentTransaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentTransactionRepository {
    Flux<PaymentTransaction> findByPaymentIntentId(PaymentIntentId paymentIntentId);
    Mono<PaymentTransaction> findByExternalId(ExternalBillingId externalId);
}
