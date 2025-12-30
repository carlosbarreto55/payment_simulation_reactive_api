package checkout.domain.auth.service;


import checkout.common.exception.InvalidCredentialsException;
import checkout.common.exception.ResourceNotFoundException;
import checkout.common.exception.UserAlreadyExistsException;
import checkout.domain.auth.dto.AuthResponse;
import checkout.domain.auth.dto.LoginRequest;
import checkout.domain.auth.dto.RegisterRequest;
import checkout.domain.auth.entity.Role;
import checkout.domain.auth.entity.User;
import checkout.domain.auth.entity.UserRole;
import checkout.domain.auth.repository.RoleRepository;
import checkout.domain.auth.repository.UserRepository;
import checkout.domain.auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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

    @Transactional
    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(existingUser ->
                        Mono.error(new UserAlreadyExistsException("Email already registered"))) // create specific exception on global exception handler
                .switchIfEmpty(
                        createUser(request)
                                .flatMap(user ->
                                        assignRoleByDefault(user)
                                                .then(jwtService.generateAccessToken(user))
                                                .map(this::buildAuthResponse)
                                )
                )
                .cast(AuthResponse.class);
    }


    public Mono<User> createUser(RegisterRequest request) {
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
                        Mono.error(new RuntimeException("There was no Role found on Role table on the database"))
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

    private AuthResponse buildAuthResponse(String token) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);
        return response;
    }

    public String hashPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    private boolean isValidPassword(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    public Mono<AuthResponse> login(LoginRequest request) {
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
                    return jwtService.generateAccessToken(user)
                            .map(this::buildAuthResponse);
                });
    }




}
