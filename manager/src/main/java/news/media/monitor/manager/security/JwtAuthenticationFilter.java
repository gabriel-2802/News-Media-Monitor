package news.media.monitor.manager.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
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
    private static final String BEARER_PREFIX        = "Bearer ";
    private static final String CLAIMS_KEY_ROLES     = "roles";
    private static final String LOG_AUTHENTICATED    = "Authenticated '{}' via JWT";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        extractBearer(request).flatMap(tokenProvider::getClaims).ifPresent(claims -> {
            if (Objects.isNull(SecurityContextHolder.getContext().getAuthentication())) {
                var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authoritiesFromClaims(claims));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug(LOG_AUTHENTICATED, claims.getSubject());
            }
        });

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> authoritiesFromClaims(Claims claims) {
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

    private Optional<String> extractBearer(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(AUTHORIZATION_HEADER))
                .filter(h -> h.startsWith(BEARER_PREFIX))
                .map(h -> h.substring(BEARER_PREFIX.length()));
    }
}