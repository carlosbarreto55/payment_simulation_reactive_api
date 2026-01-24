package checkout.domain.auth.service;


import checkout.common.exception.InvalidCredentialsException;
import checkout.common.exception.InvalidTokenException;
import checkout.common.exception.ResourceNotFoundException;
import checkout.common.exception.UserAlreadyExistsException;
import checkout.domain.auth.dto.LoginResponseDto;
import checkout.domain.auth.dto.LoginRequestDto;
import checkout.domain.auth.dto.RefreshTokenRequestDto;
import checkout.domain.auth.dto.RegisterRequestDto;
import checkout.domain.auth.dto.RegisterResponseDto;
import checkout.domain.auth.entity.RefreshToken;
import checkout.domain.auth.entity.Role;
import checkout.domain.auth.entity.User;
import checkout.domain.auth.entity.UserRole;
import checkout.domain.auth.repository.RefreshTokenRepository;
import checkout.domain.auth.repository.RoleRepository;
import checkout.domain.auth.repository.UserRepository;
import checkout.domain.auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static checkout.common.ApiConstants.DEFAULT_ROLE_NAME;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder encoder;
    private final LocalDateTime refreshTokenExpirationDate = LocalDateTime.now().plusDays(1);
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public Mono<RegisterResponseDto> register(RegisterRequestDto request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(existingUser ->
                        Mono.error(new UserAlreadyExistsException("Email already registered")))
                .switchIfEmpty(
                        createUser(request)
                                .flatMap(user ->
                                        assignRoleByDefault(user)
                                                .thenReturn(buildRegisterResponse(user))
                                )
                )
                .cast(RegisterResponseDto.class);
    }

    private RegisterResponseDto buildRegisterResponse(User user) {
        RegisterResponseDto response = new RegisterResponseDto();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        return response;
    }

    public Mono<User> createUser(RegisterRequestDto request) {
        String hashedPassword = hashPassword(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private Mono<Role> assignRoleByDefault(User user) {
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

    public String hashPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    private boolean isValidPassword(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    public Mono<LoginResponseDto> login(LoginRequestDto request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.warn("Login failed: User not found for email: {}", request.getEmail());
                            return Mono.error(new InvalidCredentialsException());
                        })
                ).flatMap(user -> {
                    if (!isValidPassword(request.getPassword(), user.getPasswordHash())) {
                        log.warn("Wrong password");
                        return Mono.error(new InvalidCredentialsException("Invalid password"));
                    }

                    if (user.getEnabled() == false) {
                        log.warn("User is not enabled");
                        return Mono.error(new InvalidCredentialsException("User is disabled"));
                    }
                    return generateTokens(user)
                            .flatMap(tokens -> saveRefreshToken(tokens.getT2(), user)
                                    .thenReturn(buildLoginResponse(tokens.getT1(), tokens.getT2())));
                });
    }

    private LoginResponseDto buildLoginResponse(String accessToken, String refreshToken){
        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);

        return response;
    }

    public Mono<Tuple2<String, String>> generateTokens(User user) {
        return Mono.zip(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        );
    }

    public Mono<Void> saveRefreshToken(String refreshToken, User user) {
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expirationDate(refreshTokenExpirationDate)
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
                                        .flatMap(user -> {
                                            return generateTokens(user)
                                                    .flatMap(tokens -> {
                                                        String newAccessToken = tokens.getT1();
                                                        String newRefreshToken = tokens.getT2();
                                                        return saveRefreshToken(newRefreshToken, user)
                                                                .then(revokeRefreshToken(request.getRefreshToken()))
                                                                .thenReturn(buildLoginResponse(newAccessToken, newRefreshToken));
                                                    });
                                        });
                            });
                });
    }

    private static Boolean isTokenExpired(RefreshToken refreshToken) {
        if (refreshToken == null) return true;
        return refreshToken.getExpirationDate().isBefore(LocalDateTime.now());
    }

    private static Boolean isTokenRevoked(RefreshToken refreshToken) {
        return Boolean.TRUE.equals(refreshToken.getRevoked());
    }

    private Mono<Void> revokeRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .flatMap(refreshToken -> {
                    refreshToken.setRevoked(true);
                    return refreshTokenRepository.save(refreshToken)
                            .then();
                });
    }

    public Mono<Void> logout (RefreshTokenRequestDto request) {
        return refreshTokenRepository.findByToken(request.getRefreshToken())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Token not found")))
                .filter (refreshToken -> !isTokenRevoked(refreshToken) && !isTokenExpired(refreshToken))
                .flatMap(refreshToken -> {
                    refreshToken.setRevoked(true);
                    return refreshTokenRepository.save(refreshToken)
                            .then();
                });
    }
}