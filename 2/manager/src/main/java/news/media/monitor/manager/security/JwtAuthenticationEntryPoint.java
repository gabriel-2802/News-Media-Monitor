package news.media.monitor.manager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_STATUS    = "status";
    private static final String KEY_ERROR     = "error";
    private static final String KEY_MESSAGE   = "message";
    private static final String KEY_PATH      = "path";
    private static final String ERROR_LABEL   = "Unauthorized";

    private static final String LOG_UNAUTHORIZED = "Unauthorized access to '{}': {}";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn(LOG_UNAUTHORIZED, request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        MAPPER.writeValue(response.getOutputStream(), Map.of(
                KEY_TIMESTAMP, Instant.now().toString(),
                KEY_STATUS,    HttpServletResponse.SC_UNAUTHORIZED,
                KEY_ERROR,     ERROR_LABEL,
                KEY_MESSAGE,   authException.getMessage(),
                KEY_PATH,      request.getRequestURI()
        ));
    }
}