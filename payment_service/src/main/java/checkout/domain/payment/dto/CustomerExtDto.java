package checkout.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Customer data for the external payment provider request.
 * Used for inline customer creation during billing.
 * All fields are required when provided.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerExtDto {

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

