package app.demo.exceptions;

public class TopicNotFoundException extends Exception {
    public TopicNotFoundException(String topicName) {
        super("Topic '" + topicName + "' does not exist.");
    }
}
