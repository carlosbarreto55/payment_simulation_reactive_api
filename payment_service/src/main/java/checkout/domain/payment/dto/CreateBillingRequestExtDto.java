package checkout.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * External request DTO for creating a billing via the payment provider (PSP).
 * Maps to POST /v1/billing/create.
 * This DTO is used exclusively for communication with the external provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillingRequestExtDto {

    /**
     * Billing frequency type.
     * Available options: ONE_TIME, MULTIPLE_PAYMENTS.
     * Default: ONE_TIME.
     */
    @JsonProperty("frequency")
    private String frequency;

    /**
     * Payment methods accepted for this billing.
     * Available options: PIX, CARD.
     * Required array length: 1-2 elements. Unique items only.
     */
    @JsonProperty("methods")
    private List<String> methods;

    /**
     * List of products the customer is paying for.
     * Minimum: 1 item.
     */
    @JsonProperty("products")
    private List<ProductExtDto> products;

    /**
     * URL to redirect the customer when they click "Back".
     */
    @JsonProperty("returnUrl")
    private String returnUrl;

    /**
     * URL to redirect the customer when payment is completed.
     */
    @JsonProperty("completionUrl")
    private String completionUrl;

    /**
     * ID of an existing customer already registered in the payment provider.
     * Optional.
     */
    @JsonProperty("customerId")
    private String customerId;

    /**
     * Customer data for inline creation. If the customer does not exist, it will be created.
     * All fields are required when this object is provided.
     * Optional (either customerId or customer can be used).
     */
    @JsonProperty("customer")
    private CustomerExtDto customer;

    /**
     * If true, coupons can be applied to this billing.
     * Default: false.
     */
    @JsonProperty("allowCoupons")
    private Boolean allowCoupons;

    /**
     * List of coupon codes available for this billing.
     * Maximum: 50 items.
     */
    @JsonProperty("coupons")
    private List<String> coupons;

    /**
     * Optional external identifier from your application for this billing.
     */
    @JsonProperty("externalId")
    private String externalId;

    /**
     * Optional metadata for the billing.
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}

