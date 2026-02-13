package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentMethodNotSupportedException extends BaseException {
    public PaymentMethodNotSupportedException(String paymentMethod) {
        super(
            "Método de pagamento não suportado: " + paymentMethod,
            HttpStatus.BAD_REQUEST,
            "PAYMENT_METHOD_NOT_SUPPORTED"
        );
    }
}

