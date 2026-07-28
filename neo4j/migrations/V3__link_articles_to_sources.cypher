// Connect every Article to the NewsSource whose base_url is a prefix of the article URL.
// Idempotent — MERGE never creates duplicate relationships.
MATCH (s:NewsSource), (a:Article)
WHERE a.url STARTS WITH s.base_url
MERGE (s)-[:PUBLISHED]->(a);
