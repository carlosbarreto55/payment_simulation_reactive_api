package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends BaseException {
    public BusinessRuleException(String message) {
        super(
            message,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "BUSINESS_RULE_VIOLATION"
        );
    }
    
    public BusinessRuleException(String message, Throwable cause) {
        super(
            message,
            cause,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "BUSINESS_RULE_VIOLATION"
        );
    }
}

