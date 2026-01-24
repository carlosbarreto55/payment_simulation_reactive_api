package checkout.domain.auth.controller;

import checkout.domain.auth.dto.LoginResponseDto;
import checkout.domain.auth.dto.LoginRequestDto;
import checkout.domain.auth.dto.RefreshTokenRequestDto;
import checkout.domain.auth.dto.RegisterRequestDto;
import checkout.domain.auth.dto.RegisterResponseDto;
import checkout.domain.auth.service.AuthService;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Register request for email: {}", request.getEmail());
        return authService.register(request)
                .map(registerResponse -> ResponseEntity.status(HttpStatus.CREATED).body(registerResponse));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login request for email: {}", request.getEmail());
        return authService.login(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout (@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Logout request for refresh token: {}", request.getRefreshToken());
        return authService.logout(request)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/refresh-token")
    public Mono<ResponseEntity<LoginResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Refresh token request for refresh token: {}", request.getRefreshToken());
        return authService.refreshToken(request)
                .map(ResponseEntity::ok);
    }
}