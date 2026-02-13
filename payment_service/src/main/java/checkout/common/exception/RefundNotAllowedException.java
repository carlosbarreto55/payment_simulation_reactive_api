package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class RefundNotAllowedException extends BaseException {
    public RefundNotAllowedException(Long paymentId) {
        super(
            "Estorno não permitido para o pagamento: " + paymentId,
            HttpStatus.BAD_REQUEST,
            "REFUND_NOT_ALLOWED"
        );
    }
    
    public RefundNotAllowedException(String message) {
        super(
            message,
            HttpStatus.BAD_REQUEST,
            "REFUND_NOT_ALLOWED"
        );
    }
}

