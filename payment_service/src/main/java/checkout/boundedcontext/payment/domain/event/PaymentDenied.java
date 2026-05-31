package checkout.boundedcontext.payment.domain.event;

import checkout.common.domain.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PaymentDenied(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String paymentIntentId,
    String reason
) implements DomainEvent {

    public PaymentDenied {
        if (eventId == null) eventId = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
