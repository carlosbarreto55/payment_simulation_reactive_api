package checkout.domain.user.service;

import checkout.common.exception.InvalidTokenException;
import checkout.common.exception.ResourceNotFoundException;
import checkout.domain.user.dto.LoginResponseDto;
import checkout.domain.user.dto.RefreshTokenRequestDto;
import checkout.domain.user.entity.RefreshToken;
import checkout.domain.user.entity.Role;
import checkout.domain.user.entity.User;
import checkout.domain.user.entity.UserRole;
import checkout.domain.user.repository.RefreshTokenRepository;
import checkout.domain.user.repository.RoleRepository;
import checkout.domain.user.repository.UserRepository;
import checkout.domain.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;

import static checkout.common.ApiConstants.DEFAULT_ROLE_NAME;
import static checkout.domain.user.mapper.UserMapper.buildLoginResponse;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder encoder;

    @Value("${app.jwt.refresh-token-expiration}")
    private Long refreshTokenExpirationTime;


    public Mono<Tuple2<String, String>> generateTokens(User user) {
        return Mono.zip(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        );
    }

    public Mono<Void> saveRefreshToken(String refreshToken, User user) {
        LocalDateTime expirationDate = LocalDateTime.now().plusSeconds(refreshTokenExpirationTime);
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expirationDate(expirationDate)
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token).then();
    }

    @Transactional
    public Mono<LoginResponseDto> refreshToken(RefreshTokenRequestDto request) {
        return jwtService.validateRefreshToken(request.getRefreshToken())
                .flatMap(claims -> {
                    Long userId = claims.get("userId", Long.class);
                    return refreshTokenRepository.findByToken(request.getRefreshToken())
                            .switchIfEmpty(Mono.error(new InvalidTokenException("Refresh token not found")))
                            .flatMap(refreshToken -> {
                                if (isTokenExpired(refreshToken) || isTokenRevoked(refreshToken)) {
                                    return Mono.error(new InvalidTokenException("Refresh token expired or revoked"));
                                }
                                return userRepository.findById(userId)
                                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                                        .flatMap(user -> generateTokens(user)
                                                .flatMap(tokens -> {
                                                    String newAccessToken = tokens.getT1();
                                                    String newRefreshToken = tokens.getT2();
                                                    return saveRefreshToken(newRefreshToken, user)
                                                            .then(revokeRefreshToken(request.getRefreshToken()))
                                                            .thenReturn(buildLoginResponse(newAccessToken, newRefreshToken));
                                                }));
                            });
                });
    }

    public static Boolean isTokenExpired(RefreshToken refreshToken) {
        if (refreshToken == null) return true;
        return refreshToken.getExpirationDate().isBefore(LocalDateTime.now());
    }

    public static Boolean isTokenRevoked(RefreshToken refreshToken) {
        return Boolean.TRUE.equals(refreshToken.getRevoked());
    }

    public Mono<Void> revokeRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .flatMap(refreshToken -> {
                    refreshToken.setRevoked(true);
                    return refreshTokenRepository.save(refreshToken)
                            .then();
                });
    }

    public String hashPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean isValidPassword(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }

    public Mono<Role> assignRoleByDefault(User user) {
        return roleRepository.findByName(DEFAULT_ROLE_NAME)
                .switchIfEmpty(
                        Mono.error(new ResourceNotFoundException("There was no Role found on Role table on the database"))
                )
                .flatMap(role -> {
                    UserRole userRole = UserRole.builder()
                            .userId(user.getId())
                            .roleId(role.getId())
                            .build();
                    return userRoleRepository.save(userRole)
                            .thenReturn(role);
                });
    }

}
