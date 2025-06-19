package app.demo.exceptions;

public class TopicDoesNotExistException extends Exception {
    public TopicDoesNotExistException(String topicName) {
        super("Topic '" + topicName + "' does not exist.");
    }
}
