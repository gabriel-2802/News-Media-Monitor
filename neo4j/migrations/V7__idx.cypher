// ═══════════════════════════════════════════════════════════════════════════
//  NewsSource
// ═══════════════════════════════════════════════════════════════════════════

CREATE CONSTRAINT news_source_name_unique IF NOT EXISTS
FOR (n:NewsSource)
REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT news_source_rss_url_unique IF NOT EXISTS
FOR (n:NewsSource)
REQUIRE n.rssUrl IS UNIQUE;

CREATE INDEX news_source_base_url_idx IF NOT EXISTS
FOR (n:NewsSource)
ON (n.baseUrl);

// ═══════════════════════════════════════════════════════════════════════════
//  Article
// ═══════════════════════════════════════════════════════════════════════════

CREATE CONSTRAINT article_url_unique IF NOT EXISTS
FOR (a:Article)
REQUIRE a.url IS UNIQUE;

CREATE INDEX article_published_at_idx IF NOT EXISTS
FOR (a:Article)
ON (a.publishedAt);

CREATE INDEX article_author_idx IF NOT EXISTS
FOR (a:Article)
ON (a.author);

CREATE INDEX article_title_idx IF NOT EXISTS
FOR (a:Article)
ON (a.title);