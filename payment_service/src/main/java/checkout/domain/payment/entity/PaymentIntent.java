package checkout.domain.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a payment intent.
 * Maps to the {@code payment_intents} table (V4 migration).
 * <p>
 * State flow: PENDING → PROCESSING → APPROVED | DENIED | REFUNDED
 */
@Table("payment_intents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntent {

    @Id
    private Long id;


    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    @Builder.Default
    private String currency = "BRL";

    @Column("status")
    @Builder.Default
    private String status = "PENDING";

    @Column("payment_method")
    private String paymentMethod;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
}

