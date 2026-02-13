package checkout.domain.user.repository;

import checkout.domain.user.entity.UserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRoleRepository extends ReactiveCrudRepository<UserRole, Long> {
}
