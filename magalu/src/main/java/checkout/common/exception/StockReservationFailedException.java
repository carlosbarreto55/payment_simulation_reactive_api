package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class StockReservationFailedException extends BaseException {
    public StockReservationFailedException(String productId) {
        super(
            "Falha na reserva de estoque para o produto: " + productId,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "STOCK_RESERVATION_FAILED"
        );
    }
    
    public StockReservationFailedException(String message, Throwable cause) {
        super(
            message,
            cause,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "STOCK_RESERVATION_FAILED"
        );
    }
}

