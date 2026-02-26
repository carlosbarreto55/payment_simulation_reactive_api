package checkout.domain.payment.enums;

/**
 * Supported payment methods for billing creation.
 * Sent as a list in the billing request — at least one must be provided.
 */
public enum PaymentMethod {
    PIX,
    CARD
}

