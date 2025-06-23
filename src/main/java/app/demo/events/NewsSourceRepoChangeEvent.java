package app.demo.events;

import lombok.Data;
import lombok.Getter;

/**
 * Event representing a change in the news source repository.
 * This event is triggered when a news source is created, updated, or deleted.
 */
public record NewsSourceRepoChangeEvent() {
}
