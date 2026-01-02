package checkout.domain.auth.repository;

import checkout.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {

    Mono<RefreshToken> findByToken(String token);

    Flux<RefreshToken> findByUserId(Long userId);

    Mono<Void> deleteByUserId(Long userId);

    Mono<Void> deleteByToken(String token);

}
