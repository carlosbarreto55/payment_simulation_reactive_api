package checkout.boundedcontext.payment.domain;

public record PaymentIntentId(Long value) {

    public PaymentIntentId {
        if (value == null) {
            throw new IllegalArgumentException("PaymentIntentId cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("PaymentIntentId must be positive");
        }
    }

    public static PaymentIntentId of(Long value) {
        return new PaymentIntentId(value);
    }
}
