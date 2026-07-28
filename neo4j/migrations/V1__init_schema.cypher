// ═══════════════════════════════════════════════════════════════════════════
//  NewsSource
//  Properties: name (String), rss_url (String), base_url (String)
// ═══════════════════════════════════════════════════════════════════════════

CREATE CONSTRAINT news_source_name_unique IF NOT EXISTS
FOR (n:NewsSource)
REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT news_source_rss_url_unique IF NOT EXISTS
FOR (n:NewsSource)
REQUIRE n.rss_url IS UNIQUE;

CREATE INDEX news_source_base_url_idx IF NOT EXISTS
FOR (n:NewsSource)
ON (n.base_url);

// ═══════════════════════════════════════════════════════════════════════════
//  Article
//  Properties: url (String), title (String), author (String),
//              published_at (LocalDateTime), body_text (String),
//              description (String)
// ═══════════════════════════════════════════════════════════════════════════

CREATE CONSTRAINT article_url_unique IF NOT EXISTS
FOR (a:Article)
REQUIRE a.url IS UNIQUE;

CREATE INDEX article_url_idx IF NOT EXISTS
FOR (a:Article)
ON (a.url);

// range index supports time-based ORDER BY / WHERE queries
CREATE INDEX article_published_at_idx IF NOT EXISTS
FOR (a:Article)
ON (a.published_at);

CREATE INDEX article_author_idx IF NOT EXISTS
FOR (a:Article)
ON (a.author);

CREATE INDEX article_title_idx IF NOT EXISTS
FOR (a:Article)
ON (a.title);
