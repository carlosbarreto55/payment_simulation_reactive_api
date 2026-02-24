package checkout.domain.client;

import checkout.domain.payment.dto.CreateBillingRequestExtDto;
import checkout.domain.payment.dto.CreateBillingResponseExtDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class PaymentServiceClient {

    private final WebClient paymentProviderWebClient;

    public PaymentServiceClient(@Qualifier("paymentProviderWebClient") WebClient paymentProviderWebClient) {
        this.paymentProviderWebClient = paymentProviderWebClient;
    }
    public Mono<CreateBillingResponseExtDto> createBilling(CreateBillingRequestExtDto request) {
        log.info("Sending billing creation request to payment provider. externalId={}", request.getExternalId());
        return paymentProviderWebClient.post()
                .uri("/v1/billing/create")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CreateBillingResponseExtDto.class)
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Payment provider returned error. status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()));
    }

}
