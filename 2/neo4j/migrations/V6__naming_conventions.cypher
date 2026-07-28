

// Migrate Article properties to camelCase
MATCH (a:Article)
SET a.bodyText    = coalesce(a.body_text, a.bodyText),
    a.publishedAt = coalesce(a.published_at, a.publishedAt)
REMOVE a.body_text, a.published_at;