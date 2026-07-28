package news.media.monitor.manager.exceptions.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Thrown when the news provider service does not respond with HTTP 200 OK.
 * Indicates a failure in the provider's subscription operation.
 */
@Getter
public class InvalidProviderResponseException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public InvalidProviderResponseException(HttpStatusCode statusCode, String responseBody) {
        super(String.format("Provider returned unexpected status: %s %s", statusCode.value(), statusCode));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public InvalidProviderResponseException(String message, HttpStatus statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

}