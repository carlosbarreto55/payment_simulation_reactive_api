package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BaseException {
    public OrderNotFoundException(Long orderId) {
        super(
            "Pedido não encontrado: " + orderId,
            HttpStatus.NOT_FOUND,
            "ORDER_NOT_FOUND"
        );
    }
    
    public OrderNotFoundException(String identifier) {
        super(
            "Pedido não encontrado: " + identifier,
            HttpStatus.NOT_FOUND,
            "ORDER_NOT_FOUND"
        );
    }
}

