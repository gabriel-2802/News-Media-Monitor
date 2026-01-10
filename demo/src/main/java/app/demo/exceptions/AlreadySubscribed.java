package app.demo.exceptions;

public class AlreadySubscribed extends Exception {
    public AlreadySubscribed(String topicName) {
        super("Already subscribed to topic: " + topicName);
    }
}
