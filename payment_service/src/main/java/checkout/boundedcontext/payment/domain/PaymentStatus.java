package checkout.boundedcontext.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    APPROVED,
    DENIED,
    REFUNDED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING || target == DENIED;
            case PROCESSING -> target == APPROVED || target == DENIED;
            case APPROVED -> target == REFUNDED;
            default -> false;
        };
    }

    public boolean isTerminal() {
        return this == APPROVED || this == DENIED || this == REFUNDED;
    }
}
