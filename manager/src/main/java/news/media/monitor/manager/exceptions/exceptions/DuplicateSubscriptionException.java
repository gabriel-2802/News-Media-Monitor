package news.media.monitor.manager.exceptions.exceptions;

public class DuplicateSubscriptionException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public DuplicateSubscriptionException() {
        super("Already subscribed");
    }
}
