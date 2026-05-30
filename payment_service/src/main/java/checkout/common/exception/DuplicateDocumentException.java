package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateDocumentException extends BaseException {
    public DuplicateDocumentException(String documentNumber) {
        super(
            "Document number already exists: " + documentNumber,
            HttpStatus.CONFLICT,
            "DUPLICATE_DOCUMENT"
        );
    }
}
