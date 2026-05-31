package checkout.boundedcontext.payment.domain;

import lombok.Getter;
import java.time.Instant;
import java.util.Objects;

@Getter
public class PaymentTransaction {
    private final ExternalBillingId externalId;
    private final String providerStatus;
    private final Instant processedAt;

    private PaymentTransaction(ExternalBillingId externalId, String providerStatus) {
        Objects.requireNonNull(externalId, "externalId must not be null");
        Objects.requireNonNull(providerStatus, "providerStatus must not be null");
        if (providerStatus.isBlank()) {
            throw new IllegalArgumentException("providerStatus must not be blank");
        }
        this.externalId = externalId;
        this.providerStatus = providerStatus;
        this.processedAt = Instant.now();
    }

    public static PaymentTransaction record(ExternalBillingId externalId, String providerStatus) {
        return new PaymentTransaction(externalId, providerStatus);
    }
}
