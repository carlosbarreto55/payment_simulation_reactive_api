package checkout.domain.customer.repository;

import checkout.domain.customer.entity.Customer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer,Long> {

    @Query("SELECT COUNT(*) > 0 FROM customers WHERE document = :documentNumber")
    Mono<Boolean> existsByDocumentNumber(String documentNumber);

}
