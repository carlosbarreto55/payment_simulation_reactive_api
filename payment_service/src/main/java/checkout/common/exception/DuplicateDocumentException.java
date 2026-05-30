package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateDocumentException extends BaseException {
    public DuplicateDocumentException() {
        super(
            "Document number already exists",
            HttpStatus.CONFLICT,
            "DUPLICATE_DOCUMENT"
        );
    }
}
