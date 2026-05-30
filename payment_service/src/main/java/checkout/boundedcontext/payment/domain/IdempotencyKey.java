package checkout.boundedcontext.payment.domain;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey cannot be null or blank");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("IdempotencyKey too long");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }
}
