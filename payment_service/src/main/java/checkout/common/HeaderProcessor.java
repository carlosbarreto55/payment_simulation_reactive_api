package checkout.common;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public class HeaderProcessor {
    public static String extractToken (ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    }
}
