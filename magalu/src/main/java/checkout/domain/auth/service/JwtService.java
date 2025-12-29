package checkout.domain.auth.service;

import checkout.domain.auth.entity.Role;
import checkout.domain.auth.entity.User;
import checkout.domain.auth.repository.RoleRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {

    private final RoleRepository roleRepository;

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-time}")
    private Long accessTokenExpirationTime;

    private SecretKey getSignedKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Mono<String> generateAccessToken(User user) {
        return roleRepository.findAll()
                .collectList()
                .map(roles -> {
                    List<String> roleNames = roles.stream()
                            .map(Role::getName)
                            .toList();

                    Instant now = Instant.now();
                    Instant expirationTime = now.plusSeconds(accessTokenExpirationTime);

                    return Jwts.builder()
                            .subject(user.getEmail())
                            .claim("userId", user.getId())
                            .claim("email", user.getEmail())
                            .claim("roles", roleNames)
                            .issuedAt(Date.from(now))
                            .expiration(Date.from(expirationTime))
                            .signWith(getSignedKey())
                            .compact();
                });
    }

    public Mono<Claims> validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignedKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration().before(new Date())) {
                return Mono.error(new RuntimeException("Token expired"));
            }

            return Mono.just(claims);
        } catch (Exception e) {
            log.error("Error validating token", e);
            return Mono.error(new IllegalArgumentException("Invalid token" + e.getMessage()));
        }
    }

    public Mono<String> getUserNameFromToken(String token) {
        return validateAccessToken(token)
                .map(Claims::getSubject);
    }

    public Mono<Long> getUserIdFromToken(String token) {
        return validateAccessToken(token)
                .map(claims -> claims.get("userId", Long.class));
    }

}
