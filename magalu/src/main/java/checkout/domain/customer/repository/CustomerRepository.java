package checkout.domain.customer.repository;

import checkout.domain.customer.entity.Customer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer,Long> {

    Mono<Boolean> existsByDocumentNumber (String documentNumber);
}
