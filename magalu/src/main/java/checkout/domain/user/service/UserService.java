package checkout.domain.user.service;


import checkout.common.exception.InvalidCredentialsException;
import checkout.common.exception.ResourceNotFoundException;
import checkout.common.exception.UserAlreadyExistsException;
import checkout.domain.user.dto.LoginRequestDto;
import checkout.domain.user.dto.LoginResponseDto;
import checkout.domain.user.dto.RefreshTokenRequestDto;
import checkout.domain.user.dto.RegisterRequestDto;
import checkout.domain.user.dto.RegisterResponseDto;
import checkout.domain.user.entity.User;
import checkout.domain.user.repository.RefreshTokenRepository;
import checkout.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static checkout.domain.user.mapper.UserMapper.buildLoginResponse;
import static checkout.domain.user.mapper.UserMapper.buildRegisterResponse;
import static checkout.domain.user.service.AuthenticationService.isTokenExpired;
import static checkout.domain.user.service.AuthenticationService.isTokenRevoked;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationService authService;

    @Transactional
    public Mono<RegisterResponseDto> register(RegisterRequestDto request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(existingUser ->
                        Mono.error(new UserAlreadyExistsException("Email already registered")))
                .switchIfEmpty(
                        createUser(request)
                                .flatMap(user ->
                                        authService.assignRoleByDefault(user)
                                                .thenReturn(buildRegisterResponse(user))
                                )
                )
                .cast(RegisterResponseDto.class);
    }

    public Mono<User> createUser(RegisterRequestDto request) {
        String hashedPassword = authService.hashPassword(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    public Mono<LoginResponseDto> login(LoginRequestDto request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid email")))
                .flatMap(user -> {
                    if (validateUser(request, user)) {
                        return authService.generateTokens(user)
                                .flatMap(tokens -> authService.saveRefreshToken(tokens.getT2(), user)
                                        .thenReturn(buildLoginResponse(tokens.getT1(), tokens.getT2())));
                    } else {
                        return Mono.error(new InvalidCredentialsException("Invalid password"));
                    }
                });
    }

    public Mono<Void> logout(RefreshTokenRequestDto request) {
        return refreshTokenRepository.findByToken(request.getRefreshToken())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Token not found")))
                .filter(refreshToken -> !isTokenRevoked(refreshToken) && !isTokenExpired(refreshToken))
                .flatMap(refreshToken -> {
                    refreshToken.setRevoked(true);
                    return refreshTokenRepository.save(refreshToken)
                            .then();
                });
    }

    private Boolean validateUser(LoginRequestDto request, User user) {
        return authService.isValidPassword(request.getPassword(), user.getPasswordHash()) && user.getEnabled() == true;
    }

}