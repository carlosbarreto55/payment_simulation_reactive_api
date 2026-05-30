package checkout.domain.customer.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import checkout.common.enums.DocumentType;
import java.time.LocalDateTime;

@Table("customers")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Customer {
    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("name")
    private String name;

    @Column("email")
    private String email;

    @Column("document")
    private String documentNumber;

    @Column("document_type")
    private DocumentType documentType;

    @Column("phone_number")
    private String phoneNumber;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
}
