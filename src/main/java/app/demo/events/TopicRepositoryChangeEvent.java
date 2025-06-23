package app.demo.events;

import app.demo.entities.Topic;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopicRepositoryChangeEvent {
    private final Topic topic;
    private final boolean isDeleted; // True if the topic was deleted, false if it was created or updated
}
