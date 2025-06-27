package app.demo.events;

import app.demo.entities.Topic;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event representing a change in the topic repository.
 * @param topic The topic that was created, updated, or deleted
 * @param isDeleted True if the topic was deleted, false if it was created or updated
 */
public record TopicRepositoryChangeEvent(Topic topic, boolean isDeleted) {
}
