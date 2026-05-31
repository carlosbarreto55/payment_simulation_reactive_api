package checkout.boundedcontext.payment.domain.event;

import checkout.common.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PaymentApproved(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String paymentIntentId
) implements DomainEvent {

    public PaymentApproved {
        if (eventId == null) eventId = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
