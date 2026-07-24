package news.media.monitor.manager.exceptions.exceptions;

public class DuplicateEmailException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public DuplicateEmailException() {
        super("Email already registered");
    }
}
