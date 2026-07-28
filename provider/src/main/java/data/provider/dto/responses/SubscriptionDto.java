package data.provider.dto.responses;

import data.provider.models.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "A subscription to a story or a topic, tracking how many times it has been subscribed to.")
public record SubscriptionDto(
        @Schema(description = "Subscription ID.")
        String id,

        @Schema(description = "ID of the subscribed story, if this is a story subscription.")
        String storyId,

        @Schema(description = "Name of the subscribed topic, if this is a topic subscription.", example = "Politics")
        String topicName,

        @Schema(description = "Number of active subscriptions.", example = "1")
        int count
) {
    public SubscriptionDto(Subscription subscription) {
        this(subscription.getId(),
                Objects.nonNull(subscription.getStory()) ? subscription.getStory().getId() : null,
                Objects.nonNull(subscription.getTopic()) ? subscription.getTopic().getName() : null,
                subscription.getCount());
    }
}
