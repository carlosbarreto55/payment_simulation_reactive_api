package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends BaseException {
    public PaymentNotFoundException(Long paymentId) {
        super(
            "Pagamento não encontrado: " + paymentId,
            HttpStatus.NOT_FOUND,
            "PAYMENT_NOT_FOUND"
        );
    }
    
    public PaymentNotFoundException(String identifier) {
        super(
            "Pagamento não encontrado: " + identifier,
            HttpStatus.NOT_FOUND,
            "PAYMENT_NOT_FOUND"
        );
    }
}

