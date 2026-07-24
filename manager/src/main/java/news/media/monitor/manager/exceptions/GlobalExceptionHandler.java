package news.media.monitor.manager.exceptions;

import news.media.monitor.manager.exceptions.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import news.media.monitor.manager.exceptions.exceptions.DuplicateEmailException;
import news.media.monitor.manager.exceptions.exceptions.InvalidCredentialsException;
import news.media.monitor.manager.exceptions.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI    ERR_BASE                  = URI.create("https://api.clm-user.demo/errors/");

    private static final String SLUG_DUPLICATE_EMAIL      = "duplicate-email";
    private static final String SLUG_INVALID_CREDENTIALS  = "invalid-credentials";
    private static final String SLUG_RESOURCE_NOT_FOUND   = "resource-not-found";
    private static final String SLUG_ACCESS_DENIED        = "access-denied";
    private static final String SLUG_VALIDATION_FAILED    = "validation-failed";
    private static final String SLUG_INTERNAL_ERROR       = "internal-error";

    private static final String KEY_TIMESTAMP             = "timestamp";
    private static final String KEY_ERRORS                = "errors";
    private static final String KEY_FIELD                 = "field";
    private static final String KEY_MESSAGE               = "message";

    private static final String MSG_ACCESS_DENIED         = "Access denied";
    private static final String MSG_UNEXPECTED_ERROR      = "An unexpected error occurred";
    private static final String MSG_VALIDATION_FAILED     = "Validation failed";
    private static final String MSG_INVALID_DEFAULT       = "invalid";

    private static final String LOG_DUPLICATE_EMAIL       = "Duplicate email: {}";
    private static final String LOG_INVALID_CREDENTIALS   = "Invalid credentials: {}";
    private static final String LOG_RESOURCE_NOT_FOUND    = "Resource not found: {}";
    private static final String LOG_ACCESS_DENIED         = "Access denied: {}";
    private static final String LOG_VALIDATION_FAILED     = "Validation failed: {}";
    private static final String LOG_UNHANDLED_EXCEPTION   = "Unhandled exception";

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn(LOG_DUPLICATE_EMAIL, ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage(), SLUG_DUPLICATE_EMAIL);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn(LOG_INVALID_CREDENTIALS, ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), SLUG_INVALID_CREDENTIALS);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn(LOG_RESOURCE_NOT_FOUND, ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), SLUG_RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn(LOG_ACCESS_DENIED, ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, MSG_ACCESS_DENIED, SLUG_ACCESS_DENIED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        KEY_FIELD,   fe.getField(),
                        KEY_MESSAGE, Objects.requireNonNullElse(fe.getDefaultMessage(), MSG_INVALID_DEFAULT)
                ))
                .toList();
        log.warn(LOG_VALIDATION_FAILED, errors);
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, MSG_VALIDATION_FAILED, SLUG_VALIDATION_FAILED);
        pd.setProperty(KEY_ERRORS, errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error(LOG_UNHANDLED_EXCEPTION, ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, MSG_UNEXPECTED_ERROR, SLUG_INTERNAL_ERROR);
    }

    private ProblemDetail problem(HttpStatus status, String detail, String errorSlug) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(ERR_BASE.resolve(errorSlug));
        pd.setProperty(KEY_TIMESTAMP, Instant.now().toString());
        return pd;
    }
}