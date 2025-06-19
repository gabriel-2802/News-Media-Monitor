package app.demo.exceptions;

public class TopicAlreadyExistsException extends Exception {
    public TopicAlreadyExistsException(String topicName) {
        super("Topic '" + topicName + "' already exists.");
    }
}
