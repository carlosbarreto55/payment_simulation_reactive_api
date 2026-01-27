package checkout.config.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class JwtAuthToken extends AbstractAuthenticationToken {


    @Getter
    private final Long userId;

    @Getter
    private final String email;

    private final String token;

    public JwtAuthToken(Long userId, String email,
                        List<String> roles, String token) {
        super(convertRolesToAuthorities(roles));
        this.userId = userId;
        this.email = email;
        this.token = token;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> convertRolesToAuthorities(List<String> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return email;
    }
}
