package data.provider.services;

import data.provider.dto.messages.ArticleNotificationMessage;
import data.provider.dto.messages.NotificationType;
import data.provider.dto.messages.ScrapeJobMessage;
import data.provider.dto.requests.ArticleRequest;
import data.provider.dto.requests.TopicSetRequest;
import data.provider.dto.responses.ArticleDto;
import data.provider.exceptions.BusinessException;
import data.provider.models.Article;
import data.provider.models.NewsSource;
import data.provider.models.Topic;
import data.provider.repositories.ArticleRepository;
import data.provider.repositories.NewsSourceRepository;
import data.provider.repositories.SubscriptionRepository;
import data.provider.repositories.TopicRepository;
import data.provider.util.Constants;
import data.provider.util.FullTextSearchUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final NewsSourceRepository newsSourceRepository;
    private final TopicRepository topicRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.scrape-jobs-queue}")
    private String scrapeJobsQueue;

    @Value("${rabbitmq.article-notifications-queue}")
    private String articleNotificationsQueue;

    public List<ArticleDto> getAllArticles(final int page, final int count) {
        return articleRepository
                .findAllWithSourceAndTopic(PageRequest.of(page, count)).getContent().stream().map(ArticleDto::new).toList();
    }

    public List<ArticleDto> getAllArticlesFromSource(final int page, final int count, final String sourceName) {
        return articleRepository
                .findBySourceNameWithTopic(sourceName, PageRequest.of(page, count)).getContent().stream().map(ArticleDto::new).toList();
    }

    public List<ArticleDto> getAllArticlesWithTopic(final int page, final int count, final String topicName) {
        return articleRepository
                .findByTopicNameWithName(topicName, PageRequest.of(page, count)).getContent().stream().map(ArticleDto::new).toList();
    }

    public ArticleDto saveArticle(final ArticleRequest articleRequest) {
        if (articleRepository.existsByUrl(articleRequest.url())) {
            throw new BusinessException(Constants.ARTICLE_EXISTS_ERROR, articleRequest.url());
        }

        final NewsSource source = newsSourceRepository.findByName(articleRequest.sourceName())
                .orElseThrow(() -> new BusinessException(Constants.SOURCE_DOES_NOT_EXIST_ERROR, articleRequest.sourceName()));

        if (!stripScheme(articleRequest.url()).contains(stripScheme(source.getBaseUrl()))) {
            throw new BusinessException(Constants.NEWS_SOURCE_ARTICLE_URL_MISMATCH_ERROR, articleRequest.url(), articleRequest.sourceName());
        }

        final Article article = Article.builder()
                .author(articleRequest.author())
                .title(articleRequest.title())
                .url(articleRequest.url())
                .bodyText(articleRequest.bodyText())
                .publishedAt(articleRequest.publishedAt())
                .newsSource(source)
                .build();

        articleRepository.save(article);
        return new ArticleDto(article);
    }

    public ArticleDto getArticleByUrl(final String url) {
        final Article article = articleRepository.findByUrlWithSourceAndTopic(url)
                .orElseThrow(() -> new BusinessException(Constants.ARTICLE_DOES_NOT_EXIST_ERROR, url));
        return new ArticleDto(article);
    }

    public ArticleDto setTopic(final TopicSetRequest topicSetRequest) {
        if (!articleRepository.existsByUrl(topicSetRequest.url())) {
            throw new BusinessException(Constants.ARTICLE_DOES_NOT_EXIST_ERROR, topicSetRequest.url());
        }

        if (!topicRepository.existsByName(topicSetRequest.topic())) {
            throw new BusinessException(Constants.TOPIC_WITH_NAME_DOES_NOT_EXIST_ERROR, topicSetRequest.topic());
        }

        final Article article = articleRepository.setTopic(topicSetRequest.url(), topicSetRequest.topic());

        notifySubscribers(topicSetRequest.topic(), topicSetRequest.url());

        return new ArticleDto(article);
    }

    private void notifySubscribers(final String topicName, final String articleUrl) {
        if (!subscriptionRepository.existsByTopicName(topicName)) {
            log.info(Constants.ARTICLE_NOTIFICATION_SKIPPED_LOG, NotificationType.TOPIC, topicName, articleUrl);
            return;
        }

        rabbitTemplate.convertAndSend(articleNotificationsQueue,
                new ArticleNotificationMessage(topicName, articleUrl, NotificationType.TOPIC));
        log.info(Constants.ARTICLE_NOTIFICATION_PUBLISHED_LOG, NotificationType.TOPIC, topicName, articleUrl);
    }

    public List<ArticleDto> getArticlesByStory(final int page, final int count, final String storyId) {
        return articleRepository
                .findByStoryId(storyId, PageRequest.of(page, count)).getContent().stream().map(ArticleDto::new).toList();
    }

    public boolean existsByURL(final String url) {
        return articleRepository.existsByUrl(url);
    }

    public int triggerScrape() {
        final List<NewsSource> sources = newsSourceRepository.findAll();
        sources.forEach(source -> rabbitTemplate.convertAndSend(scrapeJobsQueue, new ScrapeJobMessage(source.getName())));

        log.info(Constants.SCRAPE_TRIGGERED_LOG, sources.size());
        return sources.size();
    }

    public List<ArticleDto> searchArticles(final String query, final int page, final int count) {
        final String fulltextQuery = FullTextSearchUtil.toPrefixQuery(query);
        return articleRepository.searchByTitleOrBody(fulltextQuery, PageRequest.of(page, count))
                .getContent().stream().map(ArticleDto::new).toList();
    }

    /**
     * RSS feeds sometimes serve http:// article links for an https-only
     * site (or vice versa) — the scheme carries no identity information for
     * "does this URL belong to this source", so strip it before comparing.
     */
    private static String stripScheme(final String url) {
        return url.replaceFirst("^https?://", "");
    }
}
