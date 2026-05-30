package checkout.boundedcontext.customer.domain;

public record CustomerId(Long value) {

    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId cannot be null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("CustomerId must be positive");
        }
    }

    public static CustomerId of(Long value) {
        return new CustomerId(value);
    }
}
