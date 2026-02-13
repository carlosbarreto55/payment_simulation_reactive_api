package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class OrderCancellationNotAllowedException extends BaseException {
    public OrderCancellationNotAllowedException(Long orderId, String currentStatus) {
        super(
            String.format("Cancelamento não permitido para o pedido %d com status: %s", 
                orderId, currentStatus),
            HttpStatus.BAD_REQUEST,
            "ORDER_CANCELLATION_NOT_ALLOWED"
        );
    }
    
    public OrderCancellationNotAllowedException(String message) {
        super(
            message,
            HttpStatus.BAD_REQUEST,
            "ORDER_CANCELLATION_NOT_ALLOWED"
        );
    }
}

