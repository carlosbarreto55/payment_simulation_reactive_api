package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException() {
        super(
            "Acesso negado",
            HttpStatus.FORBIDDEN,
            "UNAUTHORIZED"
        );
    }
    
    public UnauthorizedException(String message) {
        super(
            message,
            HttpStatus.FORBIDDEN,
            "UNAUTHORIZED"
        );
    }
}

