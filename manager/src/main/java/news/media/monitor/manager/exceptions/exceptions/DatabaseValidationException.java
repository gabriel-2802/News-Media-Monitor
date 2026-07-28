package news.media.monitor.manager.exceptions.exceptions;

public class DatabaseValidationException extends RuntimeException {
    public DatabaseValidationException(String message) {
        super(message);
    }
}
