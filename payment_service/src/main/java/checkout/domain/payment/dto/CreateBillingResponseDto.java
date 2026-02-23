package checkout.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal API response for billing creation.
 * Decoupled from the external payment provider contract.
 * Exposes only relevant data to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillingResponseDto {

    @JsonProperty("paymentIntentId")
    private Long paymentIntentId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("paymentUrl")
    private String paymentUrl;

    @JsonProperty("paymentMethods")
    private List<String> paymentMethods;

    @JsonProperty("amountInCents")
    private Integer amountInCents;
}

