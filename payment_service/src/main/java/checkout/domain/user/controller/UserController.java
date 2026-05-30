package checkout.domain.user.controller;

import checkout.domain.user.dto.LoginResponseDto;
import checkout.domain.user.dto.LoginRequestDto;
import checkout.domain.user.dto.RefreshTokenRequestDto;
import checkout.domain.user.dto.RegisterRequestDto;
import checkout.domain.user.dto.RegisterResponseDto;
import checkout.domain.user.service.AuthenticationService;
import checkout.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authService;

    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Register request for email: {}", request.getEmail());
        return userService.register(request)
                .map(registerResponse -> ResponseEntity.status(HttpStatus.CREATED).body(registerResponse));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login request for email: {}", request.getEmail());
        return userService.login(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Logout request for refresh token: {}", request.getRefreshToken());
        return userService.logout(request)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/refresh-token")
    public Mono<ResponseEntity<LoginResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Refresh token request for refresh token: {}", request.getRefreshToken());
        return authService.refreshToken(request)
                .map(ResponseEntity::ok);
    }
}