package data.provider.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIMS_KEY_ROLES = "roles";
    private static final String LOG_AUTHENTICATED = "Authenticated '{}' via JWT";

    private final JwtTokenValidator tokenValidator;

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return request.getServletPath().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final FilterChain filterChain) throws ServletException, IOException {

        extractBearer(request).flatMap(tokenValidator::getClaims).ifPresent(claims -> {
            if (Objects.isNull(SecurityContextHolder.getContext().getAuthentication())) {
                final var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authoritiesFromClaims(claims));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug(LOG_AUTHENTICATED, claims.getSubject());
            }
        });

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> authoritiesFromClaims(final Claims claims) {
        return Optional.ofNullable(claims.get(CLAIMS_KEY_ROLES))
                .filter(List.class::isInstance)
                .map(r -> (List<?>) r)
                .orElse(List.of())
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private Optional<String> extractBearer(final HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(AUTHORIZATION_HEADER))
                .filter(h -> h.startsWith(BEARER_PREFIX))
                .map(h -> h.substring(BEARER_PREFIX.length()));
    }
}
