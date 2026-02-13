package checkout.domain.user.mapper;

import checkout.domain.user.dto.LoginResponseDto;
import checkout.domain.user.dto.RegisterResponseDto;
import checkout.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public static LoginResponseDto buildLoginResponse(String accessToken, String refreshToken) {
        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);

        return response;
    }

    public static RegisterResponseDto buildRegisterResponse(User user) {
        RegisterResponseDto response = new RegisterResponseDto();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        return response;
    }

}
