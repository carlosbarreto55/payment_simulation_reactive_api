package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidPaymentStatusException extends BaseException {
    public InvalidPaymentStatusException(String currentStatus, String requiredStatus) {
        super(
            String.format("Status de pagamento inválido. Status atual: %s, Status requerido: %s", 
                currentStatus, requiredStatus),
            HttpStatus.BAD_REQUEST,
            "INVALID_PAYMENT_STATUS"
        );
    }
    
    public InvalidPaymentStatusException(String message) {
        super(
            message,
            HttpStatus.BAD_REQUEST,
            "INVALID_PAYMENT_STATUS"
        );
    }
}

