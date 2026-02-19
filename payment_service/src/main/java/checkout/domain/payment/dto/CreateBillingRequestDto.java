package checkout.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a billing via the payment provider.
 * Maps to POST /v1/billing/create.
 *
 * @see <a href="https://docs.abacatepay.com/pages/payment/create">Official Documentation</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillingRequestDto {

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
    private List<ProductDto> products;

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
    private CustomerDto customer;

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

    /**
     * Product item in the billing.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {

        /**
         * Product ID in your system. Used to automatically create the product
         * in the payment provider. Must be unique.
         */
        @JsonProperty("externalId")
        private String externalId;

        /**
         * Product name.
         */
        @JsonProperty("name")
        private String name;

        /**
         * Detailed product description. Optional.
         */
        @JsonProperty("description")
        private String description;

        /**
         * Quantity being purchased. Minimum: 1.
         */
        @JsonProperty("quantity")
        private Integer quantity;

        /**
         * Unit price in cents (BRL). Minimum: 100 (R$ 1.00).
         * Example: 2000 = R$ 20.00.
         */
        @JsonProperty("price")
        private Integer price;
    }

    /**
     * Customer data for inline creation during billing.
     * All fields are required when provided.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDto {

        /**
         * Customer full name.
         * Example: "Daniel Lima"
         */
        @JsonProperty("name")
        private String name;

        /**
         * Customer cellphone.
         * Example: "(11) 4002-8922"
         */
        @JsonProperty("cellphone")
        private String cellphone;

        /**
         * Customer email.
         * Example: "daniel_lima@abacatepay.com"
         */
        @JsonProperty("email")
        private String email;

        /**
         * Customer CPF or CNPJ.
         * Example: "123.456.789-01"
         */
        @JsonProperty("taxId")
        private String taxId;
    }
}
