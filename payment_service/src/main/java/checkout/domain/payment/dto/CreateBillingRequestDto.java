package checkout.domain.payment.dto;

import checkout.domain.payment.enums.Frequency;
import checkout.domain.payment.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal request DTO received from our API client to create a billing.
 * This is NOT the same as the external provider request.
 * The PaymentMapper is responsible for converting this into the provider-specific DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillingRequestDto {

    /**
     * Payment methods requested by the client.
     * Available options: PIX, CARD.
     */
    @NotEmpty(message = "At least one payment method is required")
    @Size(max = 2, message = "Maximum of 2 payment methods allowed")
    private List<PaymentMethod> methods;

    /**
     * Billing frequency type.
     * Available options: ONE_TIME, MULTIPLE_PAYMENTS.
     */
    @NotNull(message = "Frequency is required")
    @Builder.Default
    private Frequency frequency = Frequency.ONE_TIME;

    /**
     * Products the customer is paying for.
     */
    @NotEmpty(message = "At least one product is required")
    @Valid
    private List<ProductItemDto> products;

    /**
     * URL to redirect the customer after payment completion.
     */
    @NotBlank(message = "Return URL is required")
    private String returnUrl;

    /**
     * URL to redirect the customer when payment is completed successfully.
     */
    @NotBlank(message = "Completion URL is required")
    private String completionUrl;

    /**
     * Returns payment method names as strings for external provider communication.
     */
    public List<String> getMethodsAsStrings() {
        if (methods == null) return List.of();
        return methods.stream().map(Enum::name).toList();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductItemDto {

        @NotBlank(message = "Product external ID is required")
        private String externalId;

        @NotBlank(message = "Product name is required")
        private String name;

        private String description;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Price is required")
        @Min(value = 100, message = "Price must be at least 100 (R$ 1.00 in cents)")
        private Integer price;
    }
}
