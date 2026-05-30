package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {
    public UserAlreadyExistsException(String email) {
        super(
            "Email already registered: " + email,
            HttpStatus.CONFLICT,
            "USER_ALREADY_EXISTS"
        );
    }
}

