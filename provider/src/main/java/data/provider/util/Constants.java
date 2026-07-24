package data.provider.util;

public final class Constants {

    private Constants() {}

    // errors
    public static final String ARTICLE_EXISTS_ERROR = "Article with the URL = {} already exists.";
    public static final String SOURCE_DOES_NOT_EXIST_ERROR = "News source with the name = {} does not exist.";
    public static final String NEWS_SOURCE_ARTICLE_URL_MISMATCH_ERROR = "Article with URL = {} is not from source = {}";
    public static final String NEWS_SOURCE_BASE_URL_EXISTS_ERROR = "News source with base URL = {} already exists.";
    public static final String NEWS_SOURCE_NAME_EXISTS_ERROR = "News source with name = {} already exists.";
    public static final String NEWS_SOURCE_RSS_URL_EXISTS_ERROR = "News source with RSS URL = {} already exists.";
    public static final String NEWS_SOURCE_URLS_UNREACHABLE_ERROR = "News source URLs are not reachable: baseUrl = {}, rssUrl = {}.";
    public static final String NEWS_SOURCE_DOMAIN_MISMATCH_ERROR = "RSS URL domain does not match base URL domain: baseUrl = {}, rssUrl = {}.";
    public static final String ARTICLE_DOES_NOT_EXIST_ERROR = "Article with the URL = {} does not exist.";
    public static final String TOPIC_WITH_NAME_DOES_NOT_EXIST_ERROR = "Topic with the name = {} does not exist.";
    public static final String SUBSCRIPTION_TO_STORY_DOES_NOT_EXIST_ERROR = "Subscription to story with id = {} does not exist.";
    public static final String SUBSCRIPTION_TO_TOPIC_DOES_NOT_EXIST_ERROR = "Subscription to topic with name = {} does not exist.";

    // logs
    public static final String NEWS_SOURCE_ADD_SUCCESS_LOG = "News source saved successfully: name={}, id={}";
    public static final String NEWS_SOURCE_UPDATE_SUCCESS_LOG = "News source updated successfully: name={}, id={}";
    public static final String V1_CLASSIFIER_CREATED_LOG = "ClassificationEngineV1 initialized with API URL: {}";
    public static final String SCRAPE_TRIGGERED_LOG = "Scrape triggered: queued {} job(s) on the scrape jobs queue.";
    public static final String ARTICLE_NOTIFICATION_PUBLISHED_LOG = "Published {} notification: name={}, articleUrl={}";
    public static final String ARTICLE_NOTIFICATION_SKIPPED_LOG = "No subscription for {} '{}' — skipping notification for article {}";

    // paths
    public static final String ARTICLES_BASE_PATH = "/api/articles";
    public static final String ARTICLES_BY_SOURCE_PATH = "/source/{sourceName}";
    public static final String ARTICLES_EXISTS_PATH = "/exists";
    public static final String NEWS_SOURCES_BASE_PATH = "/api/news-sources";
    public static final String NEWS_SOURCE_BY_NAME_PATH = "/{sourceName}";
    public static final String NEWS_SOURCE_FAILURE_PATH = "/{sourceName}/failure";
    public static final String NEWS_SOURCE_RESET_PATH = "/{sourceName}/reset";
    public static final String ARTICLES_BY_TOPIC_PATH = "/topic/{topicName}";
    public static final String ARTICLES_BY_URL_PATH = "/by-url";
    public static final String ARTICLES_SET_TOPIC_PATH = "/topic";
    public static final String ARTICLES_BY_STORY_PATH = "/story/{storyId}";
    public static final String ARTICLES_TRIGGER_SCRAPE_PATH = "/trigger-scrape";

    // story paths
    public static final String STORIES_BASE_PATH = "/api/stories";
    public static final String STORIES_ATTACH_PATH = "/{storyId}/attach";
    public static final String STORIES_RECENT_PATH = "/recent";
    public static final String STORIES_BY_ARTICLE_PATH = "/by-article";
    public static final String STORIES_EXISTS_PATH = "/exists";

    // story errors
    public static final String STORY_DOES_NOT_EXIST_ERROR = "Story with id = {} does not exist.";
    public static final String ARTICLE_NOT_IN_STORY_ERROR = "Article with URL = {} is not attached to any story.";

    // search paths
    public static final String STORIES_SEARCH_PATH = "/search";
    public static final String ARTICLES_SEARCH_PATH = "/search";

    // topic paths
    public static final String TOPICS_BASE_PATH = "/api/topics";
    public static final String TOPICS_EXISTS_PATH = "/exists";

    // subscription paths
    public static final String SUBSCRIPTIONS_BASE_PATH = "/api/subscriptions";
    public static final String SUBSCRIPTIONS_STORY_PATH = "/story/{storyId}";
    public static final String SUBSCRIPTIONS_TOPIC_PATH = "/topic/{topicName}";

    // roles (short form, for use with hasRole/hasAnyRole — Spring prepends "ROLE_")
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SYSTEM = "SYSTEM";

    // fulltext index names
    public static final String STORY_TITLE_FULLTEXT_IDX = "story_title_fulltext_idx";
    public static final String ARTICLE_SEARCH_FULLTEXT_IDX = "article_search_fulltext_idx";

    // aux
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_COUNT = "20";
}

