package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateSkuException extends BaseException {
    public DuplicateSkuException(String sku) {
        super(
            "Product with SKU " + sku + " already exists",
            HttpStatus.CONFLICT,
            "DUPLICATE_SKU"
        );
    }
}
