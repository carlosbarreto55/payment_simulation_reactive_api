package checkout.domain.payment.mapper;

import checkout.domain.payment.dto.CreateBillingRequestDto;
import checkout.domain.payment.dto.CreateBillingRequestExtDto;
import checkout.domain.payment.dto.CreateBillingResponseDto;
import checkout.domain.payment.dto.CreateBillingResponseExtDto;
import checkout.domain.payment.dto.ProductExtDto;
import checkout.domain.payment.entity.PaymentIntent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper responsible for translating between external provider DTOs
 * and internal application DTOs in the payment domain.
 * Keeps the infrastructure layer decoupled from the interface layer.
 */
@Component
public class PaymentMapper {

    /**
     * Maps the internal client request into the external provider request DTO.
     *
     * @param request the internal request received from our API client
     * @return the external request DTO expected by the payment provider
     */
    public CreateBillingRequestExtDto toExternalRequest(CreateBillingRequestDto request) {
        List<ProductExtDto> products = request.getProducts().stream()
                .map(item -> ProductExtDto.builder()
                        .externalId(item.getExternalId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return CreateBillingRequestExtDto.builder()
                .frequency(request.getFrequency().name())
                .methods(request.getMethodsAsStrings())
                .products(products)
                .returnUrl(request.getReturnUrl())
                .completionUrl(request.getCompletionUrl())
                .build();
    }

    /**
     * Maps the external payment provider response and the local PaymentIntent
     * into the internal API response DTO.
     *
     * @param intent      the persisted PaymentIntent entity
     * @param extResponse the raw response from the external payment provider
     * @return the internal response DTO exposed to the client
     */
    public CreateBillingResponseDto toPaymentResponseDto(PaymentIntent intent,
                                                         CreateBillingResponseExtDto extResponse) {
        CreateBillingResponseExtDto.BillingDataDto data =
                extResponse != null ? extResponse.getData() : null;

        return CreateBillingResponseDto.builder()
                .paymentIntentId(intent.getId())
                .status(intent.getStatus())
                .paymentUrl(data != null ? data.getUrl() : null)
                .paymentMethods(data != null ? data.getMethods() : null)
                .amountInCents(data != null ? data.getAmount() : null)
                .build();
    }
}

