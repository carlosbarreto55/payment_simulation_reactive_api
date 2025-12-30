package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException(String message) {
        super(
            "Token inválido: " + message,
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN"
        );
    }
}

