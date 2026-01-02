package checkout.domain.auth.dto;

import lombok.Data;

@Data
public class LoginResponseDto {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;

}
