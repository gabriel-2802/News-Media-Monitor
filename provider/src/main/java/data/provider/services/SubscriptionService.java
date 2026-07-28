package data.provider.services;

import data.provider.dto.responses.SubscriptionDto;
import data.provider.exceptions.BusinessException;
import data.provider.models.Story;
import data.provider.models.Subscription;
import data.provider.models.Topic;
import data.provider.repositories.StoryRepository;
import data.provider.repositories.SubscriptionRepository;
import data.provider.repositories.TopicRepository;
import data.provider.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StoryRepository storyRepository;
    private final TopicRepository topicRepository;

    public SubscriptionDto subscribeToStory(final String storyId) {
        final Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BusinessException(Constants.STORY_DOES_NOT_EXIST_ERROR, storyId));

        final Subscription subscription = subscriptionRepository.findByStoryId(storyId)
                .map(this::increment)
                .orElseGet(() -> Subscription.builder().story(story).count(1).build());

        return new SubscriptionDto(subscriptionRepository.save(subscription));
    }

    public SubscriptionDto subscribeToTopic(final String topicName) {
        final Topic topic = topicRepository.findByName(topicName)
                .orElseThrow(() -> new BusinessException(Constants.TOPIC_WITH_NAME_DOES_NOT_EXIST_ERROR, topicName));

        final Subscription subscription = subscriptionRepository.findByTopicName(topicName)
                .map(this::increment)
                .orElseGet(() -> Subscription.builder().topic(topic).count(1).build());

        return new SubscriptionDto(subscriptionRepository.save(subscription));
    }

    public void unsubscribeFromStory(final String storyId) {
        final Subscription subscription = subscriptionRepository.findByStoryId(storyId)
                .orElseThrow(() -> new BusinessException(Constants.SUBSCRIPTION_TO_STORY_DOES_NOT_EXIST_ERROR, storyId));

        decrementOrDelete(subscription);
    }

    public void unsubscribeFromTopic(final String topicName) {
        final Subscription subscription = subscriptionRepository.findByTopicName(topicName)
                .orElseThrow(() -> new BusinessException(Constants.SUBSCRIPTION_TO_TOPIC_DOES_NOT_EXIST_ERROR, topicName));

        decrementOrDelete(subscription);
    }

    private Subscription increment(final Subscription subscription) {
        subscription.setCount(subscription.getCount() + 1);
        return subscription;
    }

    private void decrementOrDelete(final Subscription subscription) {
        if (subscription.getCount() <= 1) {
            subscriptionRepository.delete(subscription);
        } else {
            subscription.setCount(subscription.getCount() - 1);
            subscriptionRepository.save(subscription);
        }
    }
}
