package checkout.boundedcontext.payment.domain.port;

import checkout.boundedcontext.payment.domain.PaymentMethod;
import checkout.boundedcontext.payment.domain.ProductItem;

import java.util.List;

public record CreateBillingDomainRequest(
    List<PaymentMethod> methods,
    String frequency,
    CustomerDomainData customer,
    List<ProductItem> products,
    String returnUrl,
    String completionUrl,
    String externalId
) {}
