package checkout.domain.product.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("price")
    private BigDecimal price;

    @Column("currency")
    @Builder.Default
    private String currency = "BRL";

    @Column("sku")
    private String sku;

    @Column("stock_quantity")
    private Integer stockQuantity;

    @Column("active")
    @Builder.Default
    private Boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    public boolean hasAvailableStock(int requestedQuantity) {
        return this.stockQuantity != null && this.stockQuantity >= requestedQuantity;
    }

    public void decreaseStock(int quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new IllegalStateException("Insufficient stock for product: " + this.id);
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.stockQuantity += quantity;
    }
}

