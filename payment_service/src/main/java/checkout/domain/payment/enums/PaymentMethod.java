package checkout.domain.payment.enums;

/**
 * Supported payment methods for billing creation.
 * Sent as a list in the billing request — at least one must be provided.
 *
 * @deprecated This enum is part of the legacy domain package and will be replaced by
 *             {@link checkout.boundedcontext.payment.domain.PaymentMethod} in Phase 3.
 */
@Deprecated
public enum PaymentMethod {
    PIX,
    CARD
}

