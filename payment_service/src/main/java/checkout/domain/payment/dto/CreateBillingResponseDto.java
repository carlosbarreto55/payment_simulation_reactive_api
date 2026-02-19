package checkout.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for billing creation from the payment provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateBillingResponseDto {

    /**
     * Billing data (present on success, null on error).
     */
    @JsonProperty("data")
    private BillingDataDto data;

    /**
     * Error message (null on success).
     * On 401: simple string
     */
    @JsonProperty("error")
    private String error;

    /**
     * Billing data returned by the payment provider on successful creation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BillingDataDto {

        /**
         * Unique billing identifier from the payment provider.
         * Example: "bill_123456"
         */
        @JsonProperty("id")
        private String id;

        /**
         * URL where the customer can complete the payment.
         * Example: "https://pay.abacatepay.com/bill-5678"
         */
        @JsonProperty("url")
        private String url;

        /**
         * Current billing status.
         * Possible values: PENDING, EXPIRED, CANCELLED, PAID, REFUNDED.
         */
        @JsonProperty("status")
        private String status;

        /**
         * Indicates if the billing was created in dev/test mode.
         */
        @JsonProperty("devMode")
        private Boolean devMode;

        /**
         * Payment methods supported for this billing.
         * Values: PIX, CARD.
         */
        @JsonProperty("methods")
        private List<String> methods;

        /**
         * Products included in this billing.
         */
        @JsonProperty("products")
        private List<ProductResponseDto> products;

        /**
         * Billing frequency.
         * Values: ONE_TIME, MULTIPLE_PAYMENTS.
         */
        @JsonProperty("frequency")
        private String frequency;

        /**
         * Total amount in cents. DEPRECATED by the payment provider.
         * Example: 4000 = R$ 40.00.
         */
        @Deprecated
        @JsonProperty("amount")
        private Integer amount;

        /**
         * Next billing date-time for recurring billings, or null for one-time billings.
         */
        @JsonProperty("nextBilling")
        private String nextBilling;

        /**
         * Customer associated with this billing. May be null.
         */
        @JsonProperty("customer")
        private CustomerResponseDto customer;

        /**
         * Whether coupons can be used. May be null.
         */
        @JsonProperty("allowCoupons")
        private Boolean allowCoupons;

        /**
         * List of available coupon codes. May be null or empty.
         */
        @JsonProperty("coupons")
        private List<String> coupons;

        /**
         * When billing was created (ISO 8601).
         */
        @JsonProperty("createdAt")
        private String createdAt;

        /**
         * When billing was last updated (ISO 8601).
         */
        @JsonProperty("updatedAt")
        private String updatedAt;
    }

    /**
     * Customer data in the billing response.
     * Note: customer fields (name, cellphone, email, taxId) are nested inside a "metadata" object.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerResponseDto {

        /**
         * Unique customer identifier in the payment provider.
         * Example: "cust_123456"
         */
        @JsonProperty("id")
        private String id;

        /**
         * Customer personal data, nested inside metadata.
         */
        @JsonProperty("metadata")
        private CustomerMetadataDto metadata;
    }

    /**
     * Customer metadata containing personal information.
     * This is the actual structure returned by the payment provider inside customer.metadata.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerMetadataDto {

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

    /**
     * Product data in the billing response.
     * Note: response only includes id, externalId, and quantity (no name, description, or price).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductResponseDto {

        /**
         * Unique product identifier in the payment provider.
         * Example: "prod_123456"
         */
        @JsonProperty("id")
        private String id;

        /**
         * External product ID from your system.
         * Example: "prod-1234"
         */
        @JsonProperty("externalId")
        private String externalId;

        /**
         * Quantity of this product in the billing.
         */
        @JsonProperty("quantity")
        private Integer quantity;
    }
}
