package checkout.config.security;

import checkout.common.HeaderProcessor;
import checkout.domain.user.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthWebFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String token = HeaderProcessor.extractToken(exchange);

        if (isPublicPath(path)) {
            log.debug("Skipping authentication for public path: {}", path);
            return chain.filter(exchange);
        }

        if (token != null) {
            return jwtService.validateAccessToken(token)
                    .flatMap(claims -> {
                        JwtAuthToken authentication = createAuthentication(claims, token);
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                    })
                    .onErrorResume(e -> {
                        log.warn("An error occurred validating token");
                        return chain.filter(exchange);
                    });
        } else {
            log.warn("No token found in request");
            return chain.filter(exchange);
        }
    }

    public JwtAuthToken createAuthentication(Claims claims, String token) {
        Long userId = claims.get("userId", Long.class);
        String email = claims.get("email", String.class);
        List<String> roles = claims.get("roles", List.class);

        return new JwtAuthToken(userId, email, roles, token);
    }

    private Boolean isPublicPath(String path) {
        return path.equals("/api/auth/register") ||
                path.equals("/api/auth/login") ||
                path.equals("/api/auth/refresh-token") ||
                path.equals("/actuator/health");
    }
}
