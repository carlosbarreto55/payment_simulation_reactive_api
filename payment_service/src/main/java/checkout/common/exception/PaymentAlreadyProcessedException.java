package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentAlreadyProcessedException extends BaseException {
    public PaymentAlreadyProcessedException(Long paymentId) {
        super(
            "Pagamento já processado: " + paymentId,
            HttpStatus.CONFLICT,
            "PAYMENT_ALREADY_PROCESSED"
        );
    }
    
    public PaymentAlreadyProcessedException(String message) {
        super(
            message,
            HttpStatus.CONFLICT,
            "PAYMENT_ALREADY_PROCESSED"
        );
    }
}

