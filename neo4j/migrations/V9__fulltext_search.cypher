// ═══════════════════════════════════════════════════════════════════════════
//  Full-text search
// ═══════════════════════════════════════════════════════════════════════════

CREATE FULLTEXT INDEX story_title_fulltext_idx IF NOT EXISTS
FOR (s:Story)
ON EACH [s.title];

CREATE FULLTEXT INDEX article_search_fulltext_idx IF NOT EXISTS
FOR (a:Article)
ON EACH [a.title, a.bodyText];
