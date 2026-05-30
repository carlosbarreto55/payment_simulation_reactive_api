package checkout.boundedcontext.payment.domain;

public record ExternalBillingId(String value) {

    public ExternalBillingId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ExternalBillingId cannot be null or blank");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("ExternalBillingId too long");
        }
    }

    public static ExternalBillingId of(String value) {
        return new ExternalBillingId(value);
    }
}
