package checkout.boundedcontext.payment.domain;

import java.util.Collections;
import java.util.List;

public enum PaymentMethod {
    PIX, CARD;

    public static PaymentMethod fromAbacatePay(String value) {
        if (value == null) {
            throw new IllegalArgumentException("PaymentMethod value cannot be null");
        }
        return switch (value) {
            case "PIX" -> PIX;
            case "CARD" -> CARD;
            default -> throw new IllegalArgumentException("Invalid PaymentMethod: " + value);
        };
    }

    public String toAbacatePayString() {
        return switch (this) {
            case PIX -> "PIX";
            case CARD -> "CARD";
        };
    }

    public static List<PaymentMethod> validatedList(List<PaymentMethod> methods) {
        if (methods == null) {
            throw new IllegalArgumentException("PaymentMethod list cannot be null");
        }
        if (methods.isEmpty() || methods.size() > 2) {
            throw new IllegalArgumentException("PaymentMethod list must contain 1 or 2 elements");
        }
        if (methods.size() != methods.stream().distinct().count()) {
            throw new IllegalArgumentException("PaymentMethod list must contain unique elements");
        }
        return Collections.unmodifiableList(methods);
    }
}
