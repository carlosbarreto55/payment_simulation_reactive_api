package checkout.boundedcontext.payment.domain.event;

import checkout.common.domain.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentInitiated(
    UUID eventId,
    Instant occurredAt,
    String aggregateId,
    String idempotencyKey,
    int amountInCents,
    String currency,
    List<String> methods,
    Long customerId,
    List<ProductItemSnapshot> products
) implements DomainEvent {

    public PaymentInitiated {
        if (eventId == null) eventId = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public record ProductItemSnapshot(String externalId, String name, int quantity, int priceInCents) {}
}
