package checkout.domain.auth.repository;

import checkout.domain.auth.entity.Role;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RoleRepository extends ReactiveCrudRepository<Role, Long> {

    Mono<Role> findByName(String name);

}
