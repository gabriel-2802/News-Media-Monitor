
// Migrate NewsSource properties to camelCase
MATCH (n:NewsSource)
SET n.failureCount = coalesce(n.failure_count, n.failureCount, 0),
    n.isDisabled   = coalesce(n.is_disabled, n.isDisabled, false),
    n.baseUrl      = coalesce(n.base_url, n.baseUrl),
    n.rssUrl       = coalesce(n.rss_url, n.rssUrl)
REMOVE n.failure_count, n.is_disabled, n.base_url, n.rss_url;
