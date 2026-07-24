package data.provider.services;

import data.provider.dto.messages.ArticleNotificationMessage;
import data.provider.dto.messages.NotificationType;
import data.provider.dto.requests.AttachArticleRequest;
import data.provider.dto.responses.ArticleDto;
import data.provider.dto.responses.StoryDto;
import data.provider.exceptions.BusinessException;
import data.provider.models.Story;
import data.provider.repositories.ArticleRepository;
import data.provider.repositories.StoryRepository;
import data.provider.repositories.SubscriptionRepository;
import data.provider.util.Constants;
import data.provider.util.FullTextSearchUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoryService {

    /**
     * Cap on articles fetched per story when hydrating full StoryDtos (see
     * getStories). Not the same as the story-list page size — this bounds
     * how many articles come back embedded in *each* story.
     */
    private static final int MAX_ARTICLES_PER_STORY = 100;

    private final StoryRepository storyRepository;
    private final ArticleRepository articleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.article-notifications-queue}")
    private String articleNotificationsQueue;

    public StoryDto createStory(final String title) {
        final Instant now = Instant.now();
        final Story story = Story.builder()
                        .title(title)
                        .createdAt(now)
                        .lastUpdated(now)
                        .build();

        return new StoryDto(storyRepository.save(story));
    }

    public StoryDto attachArticle(final String storyId, final AttachArticleRequest request) {
        if (!storyRepository.existsById(storyId)) {
            throw new BusinessException(Constants.STORY_DOES_NOT_EXIST_ERROR, storyId);
        }
        if (!articleRepository.existsByUrl(request.articleUrl())) {
            throw new BusinessException(Constants.ARTICLE_DOES_NOT_EXIST_ERROR, request.articleUrl());
        }

        final Story updated = storyRepository.attachArticle(storyId, request.articleUrl(), Instant.now())
                .orElseThrow(() -> new BusinessException(Constants.STORY_DOES_NOT_EXIST_ERROR, storyId));

        notifySubscribers(updated, request.articleUrl());

        return new StoryDto(updated);
    }

    private void notifySubscribers(final Story story, final String articleUrl) {
        if (!subscriptionRepository.existsByStoryId(story.getId())) {
            log.info(Constants.ARTICLE_NOTIFICATION_SKIPPED_LOG, NotificationType.STORY, story.getTitle(), articleUrl);
            return;
        }

        rabbitTemplate.convertAndSend(articleNotificationsQueue,
                new ArticleNotificationMessage(story.getTitle(), articleUrl, NotificationType.STORY));
        log.info(Constants.ARTICLE_NOTIFICATION_PUBLISHED_LOG, NotificationType.STORY, story.getTitle(), articleUrl);
    }

    public List<StoryDto> getRecentStories(final int days) {
        final Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return storyRepository.findActiveSince(since).stream().map(StoryDto::new).toList();
    }

    public List<StoryDto> getStories(final int page, final int count) {
        return storyRepository.findAllPaginated(PageRequest.of(page, count))
                .getContent().stream()
                .map(story -> new StoryDto(story, getArticlesForStory(story.getId())))
                .toList();
    }

    private List<ArticleDto> getArticlesForStory(final String storyId) {
        return articleRepository.findByStoryId(storyId, PageRequest.of(0, MAX_ARTICLES_PER_STORY))
                .getContent().stream().map(ArticleDto::new).toList();
    }

    public StoryDto getStoryForArticle(final String articleUrl) {
        if (!articleRepository.existsByUrl(articleUrl)) {
            throw new BusinessException(Constants.ARTICLE_DOES_NOT_EXIST_ERROR, articleUrl);
        }

        final Story story = storyRepository.findByArticleUrl(articleUrl)
                .orElseThrow(() -> new BusinessException(Constants.ARTICLE_NOT_IN_STORY_ERROR, articleUrl));

        return new StoryDto(story, getArticlesForStory(story.getId()));
    }

    public boolean existsById(final String storyId) {
        return storyRepository.existsById(storyId);
    }

    public List<StoryDto> searchStories(final String query, final int page, final int count) {
        final String fulltextQuery = FullTextSearchUtil.toPrefixQuery(query);
        return storyRepository.searchByTitle(fulltextQuery, PageRequest.of(page, count))
                .getContent().stream().map(StoryDto::new).toList();
    }
}
