CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_articles_fts
ON articles
USING gin(to_tsvector('english', title || ' ' || content));