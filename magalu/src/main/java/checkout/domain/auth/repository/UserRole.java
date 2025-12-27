package checkout.domain.auth.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRole extends ReactiveCrudRepository<UserRole, Long> {
}
