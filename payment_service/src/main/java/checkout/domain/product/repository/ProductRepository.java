package checkout.domain.product.repository;

import checkout.domain.product.entity.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    Mono<Product> findBySku(String sku);

    Flux<Product> findByActiveTrue();

    @Query("SELECT * FROM products WHERE active = true AND stock_quantity > 0")
    Flux<Product> findAvailableProducts();

    Mono<Boolean> existsBySku(String sku);
}

