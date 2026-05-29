package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateIdempotencyKeyException extends BaseException {
    public DuplicateIdempotencyKeyException(String idempotencyKey) {
        super(
            "Idempotency key already used: " + idempotencyKey,
            HttpStatus.CONFLICT,
            "DUPLICATE_IDEMPOTENCY_KEY"
        );
    }
}

