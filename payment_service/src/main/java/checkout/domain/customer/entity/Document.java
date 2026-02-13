package checkout.domain.customer.entity;

import checkout.common.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private DocumentType documentType;
    private String documentNumber;
}
