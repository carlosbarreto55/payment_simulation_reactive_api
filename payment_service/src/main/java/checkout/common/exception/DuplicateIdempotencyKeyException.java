package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateIdempotencyKeyException extends BaseException {
    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super(
            "Chave de idempotência já utilizada: " + idempotencyKey,
            HttpStatus.CONFLICT,
            "DUPLICATE_IDEMPOTENCY_KEY"
        );
    }
}

