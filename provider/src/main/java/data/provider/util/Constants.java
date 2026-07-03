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
    public static final String NEWS_SOURCE_DOMAIN_MISMATCH_LOG = "News source domain mismatch: baseUrl={}, rssUrl={}";
    public static final String NEWS_SOURCE_DOMAIN_MISMATCH_ERROR = "RSS URL domain does not match base URL domain: baseUrl = {}, rssUrl = {}.";
    public static final String NEWS_SOURCE_ADD_SUCCESS_LOG = "News source saved successfully: name={}, id={}";

    // paths
    public static final String ARTICLES_BASE_PATH = "/api/articles";
    public static final String ARTICLES_BY_SOURCE_PATH = "/source/{sourceName}";
    public static final String ARTICLES_EXISTS_PATH = "/exists";
    public static final String NEWS_SOURCES_BASE_PATH = "/api/news-sources";
    public static final String NEWS_SOURCE_BY_NAME_PATH = "/{sourceName}";
    public static final String NEWS_SOURCE_FAILURE_PATH = "/{sourceName}/failure";
    public static final String NEWS_SOURCE_RESET_PATH = "/{sourceName}/reset";
    public static final String ARTICLES_BY_TOPIC_PATH = "/topic/{topicName}";


    // aux
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_COUNT = "20";
}

