package checkout.domain.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entity representing a payment transaction record.
 * Stores the result of each payment attempt against the external provider.
 * Maps to the {@code payment_transactions} table (V4 migration).
 * <p>
 * A PaymentIntent can have multiple transactions (e.g., retry scenarios).
 * Transaction records are immutable after creation.
 */
@Table("payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    private Long id;

    @Column("payment_intent_id")
    private Long paymentIntentId;

    @Column("external_id")
    private String externalId;

    @Column("status")
    private String status;

    @Column("failure_reason")
    private String failureReason;

    @Column("processed_at")
    private LocalDateTime processedAt;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
}

