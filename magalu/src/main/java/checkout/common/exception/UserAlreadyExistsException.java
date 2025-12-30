package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {
    public UserAlreadyExistsException(String email) {
        super(
            "Email já cadastrado: " + email,
            HttpStatus.CONFLICT,
            "USER_ALREADY_EXISTS"
        );
    }
}

