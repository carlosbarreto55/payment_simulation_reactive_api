package checkout.domain.customer.dto;

import checkout.domain.customer.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDto {
    private Long id;

    private String name;

    private String email;

    private Document document;

    private String phoneNumber;

    private LocalDateTime createdAt;
}