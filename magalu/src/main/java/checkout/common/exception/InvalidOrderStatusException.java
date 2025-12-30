package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderStatusException extends BaseException {
    public InvalidOrderStatusException(String currentStatus, String requiredStatus) {
        super(
            String.format("Status de pedido inválido. Status atual: %s, Status requerido: %s", 
                currentStatus, requiredStatus),
            HttpStatus.BAD_REQUEST,
            "INVALID_ORDER_STATUS"
        );
    }
    
    public InvalidOrderStatusException(String message) {
        super(
            message,
            HttpStatus.BAD_REQUEST,
            "INVALID_ORDER_STATUS"
        );
    }
}

