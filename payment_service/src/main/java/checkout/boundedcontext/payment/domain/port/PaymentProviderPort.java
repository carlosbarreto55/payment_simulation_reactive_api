package checkout.boundedcontext.payment.domain.port;

import checkout.boundedcontext.payment.domain.ExternalBillingId;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentProviderPort {
    Mono<ExternalBillingResponse> createBilling(CreateBillingDomainRequest request);
    Mono<ExternalBillingResponse> getBilling(ExternalBillingId billingId);
    Mono<ExternalBillingResponse> cancelBilling(ExternalBillingId billingId);
    Mono<List<ExternalBillingResponse>> listBillings();
}
