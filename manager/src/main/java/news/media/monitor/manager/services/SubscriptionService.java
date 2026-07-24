package news.media.monitor.manager.services;

import news.media.monitor.manager.dto.requests.CreateSubscriptionRequest;
import news.media.monitor.manager.dto.responses.SubscriptionResponse;
import news.media.monitor.manager.exceptions.exceptions.DuplicateSubscriptionException;
import news.media.monitor.manager.exceptions.exceptions.ExternalServiceException;
import news.media.monitor.manager.exceptions.exceptions.ResourceNotFoundException;
import news.media.monitor.manager.models.Subscription;
import news.media.monitor.manager.models.SubscriptionType;
import news.media.monitor.manager.repositories.SubscriptionRepository;
import news.media.monitor.manager.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SubscriptionService {

    private static final String USER_NOT_FOUND            = "User not found: ";
    private static final String TOPIC_NOT_FOUND            = "Topic does not exist: ";
    private static final String STORY_NOT_FOUND            = "Story does not exist: ";
    private static final String NEWS_PROVIDER_UNAVAILABLE  = "News provider service is unavailable";
    private static final String SORT_FIELD_CREATED_AT      = "createdAt";

    private static final String PATH_SUBSCRIPTIONS_STORY = "/api/subscriptions/story/{targetId}";
    private static final String PATH_SUBSCRIPTIONS_TOPIC = "/api/subscriptions/topic/{targetId}";

    private static final String LOG_SUBSCRIBED   = "User {} subscribed to {} '{}'";
    private static final String LOG_UNSUBSCRIBED = "User {} unsubscribed from {} '{}'";

    private static final String PROP_NEWS_PROVIDER_BASE_URL = "${app.news-provider.base-url}";

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository         userRepository;
    private final RestTemplate           restTemplate;
    private final String                 newsProviderBaseUrl;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               UserRepository userRepository,
                               RestTemplate restTemplate,
                               @Value(PROP_NEWS_PROVIDER_BASE_URL) String newsProviderBaseUrl) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository         = userRepository;
        this.restTemplate           = restTemplate;
        this.newsProviderBaseUrl    = newsProviderBaseUrl;
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getAll(String email, int page, int size) {
        Long userId = resolveUserId(email);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, SORT_FIELD_CREATED_AT));
        return subscriptionRepository.findByUserId(userId, pageable).map(SubscriptionResponse::from);
    }

    @Transactional
    public SubscriptionResponse subscribe(String email, CreateSubscriptionRequest request) {
        Long userId = resolveUserId(email);

        if (subscriptionRepository.existsByUserIdAndTypeAndTargetId(userId, request.type(), request.targetId())) {
            throw new DuplicateSubscriptionException();
        }

        Subscription subscription = new Subscription();
        subscription.setUser(userRepository.getReferenceById(userId));
        subscription.setType(request.type());
        subscription.setTargetId(request.targetId());

        SubscriptionResponse response = SubscriptionResponse.from(subscriptionRepository.save(subscription));
        log.info(LOG_SUBSCRIBED, userId, request.type(), request.targetId());

        providerSubscribe(request.type(), request.targetId());

        return response;
    }

    @Transactional
    public void unsubscribe(String email, String subscriptionId) {
        Long userId = resolveUserId(email);
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        subscriptionRepository.delete(subscription);
        providerUnsubscribe(subscription.getType(), subscription.getTargetId());
        log.info(LOG_UNSUBSCRIBED, userId, subscription.getType(), subscription.getTargetId());
    }

    private void providerSubscribe(SubscriptionType type, String targetId) {
        try {
            restTemplate.postForEntity(newsProviderBaseUrl + pathFor(type), null, Void.class, targetId);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new ResourceNotFoundException(notFoundMessage(type, targetId));
        } catch (RestClientException e) {
            throw new ExternalServiceException(NEWS_PROVIDER_UNAVAILABLE, e);
        }
    }

    private void providerUnsubscribe(SubscriptionType type, String targetId) {
        try {
            restTemplate.delete(newsProviderBaseUrl + pathFor(type), targetId);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new ResourceNotFoundException(notFoundMessage(type, targetId));
        } catch (RestClientException e) {
            throw new ExternalServiceException(NEWS_PROVIDER_UNAVAILABLE, e);
        }
    }

    private String pathFor(SubscriptionType type) {
        return type == SubscriptionType.TOPIC ? PATH_SUBSCRIPTIONS_TOPIC : PATH_SUBSCRIPTIONS_STORY;
    }

    private String notFoundMessage(SubscriptionType type, String targetId) {
        return (type == SubscriptionType.TOPIC ? TOPIC_NOT_FOUND : STORY_NOT_FOUND) + targetId;
    }

    private Long resolveUserId(String email) {
        return userRepository.findIdByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + email));
    }
}
