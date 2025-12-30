package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class OrderAlreadyPaidException extends BaseException {
    public OrderAlreadyPaidException(Long orderId) {
        super(
            "Pedido já foi pago: " + orderId,
            HttpStatus.CONFLICT,
            "ORDER_ALREADY_PAID"
        );
    }
    
    public OrderAlreadyPaidException(String message) {
        super(
            message,
            HttpStatus.CONFLICT,
            "ORDER_ALREADY_PAID"
        );
    }
}

