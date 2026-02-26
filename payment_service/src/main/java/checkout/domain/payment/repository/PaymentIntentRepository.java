package checkout.domain.payment.repository;

import checkout.domain.payment.entity.PaymentIntent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PaymentIntentRepository extends ReactiveCrudRepository<PaymentIntent, Long> {

    Mono<PaymentIntent> findByIdempotencyKey (String idempotencyKey);


    Mono<Boolean> existsByIdempotencyKey(String idempotencyKey);

}
