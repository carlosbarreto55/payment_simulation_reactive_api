package checkout.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product data for the external payment provider request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductExtDto {

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

