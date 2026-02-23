package checkout.domain.payment.repository;

import checkout.domain.payment.entity.PaymentTransaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentTransactionRepository extends ReactiveCrudRepository<PaymentTransaction, Long> {

    Flux<PaymentTransaction> findByPaymentIntentId(Long paymentIntentId);

    Mono<PaymentTransaction> findByExternalId(String externalId);
}
