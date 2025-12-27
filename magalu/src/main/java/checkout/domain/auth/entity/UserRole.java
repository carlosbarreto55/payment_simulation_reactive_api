package checkout.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Column("user_id")
    private Long userId;

    @Column("role_id")
    private Long roleId;

}
