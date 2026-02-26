package checkout.domain.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequestDto {

    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity must be zero or positive")
    private Integer stockQuantity;

    private Boolean active;
}

