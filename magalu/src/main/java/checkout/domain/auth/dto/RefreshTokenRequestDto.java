package checkout.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequestDto {

    @NotBlank
    private String refreshToken;

}
