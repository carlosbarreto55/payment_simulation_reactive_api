package checkout.boundedcontext.payment.domain.port;

import java.util.List;

public record ExternalBillingResponse(
    String billingId,
    String status,
    String paymentUrl,
    List<String> methods,
    String customerId,
    int amountInCents
) {}
