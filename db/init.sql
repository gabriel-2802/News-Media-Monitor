-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX IF NOT EXISTS idx_articles_fts
--     ON articles
--         USING gin(to_tsvector('english', title || ' ' || content));

INSERT INTO roles (authority)
SELECT 'ROLE_USER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE authority = 'ROLE_USER');

INSERT INTO roles (authority)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE authority = 'ROLE_ADMIN');

INSERT INTO topics (name)
SELECT 'arts'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'arts');

INSERT INTO topics (name)
SELECT 'arts  culture'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'arts  culture');

INSERT INTO topics (name)
SELECT 'black voices'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'black voices');

INSERT INTO topics (name)
SELECT 'business'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'business');

INSERT INTO topics (name)
SELECT 'college'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'college');

INSERT INTO topics (name)
SELECT 'comedy'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'comedy');

INSERT INTO topics (name)
SELECT 'crime'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'crime');

INSERT INTO topics (name)
SELECT 'culture  arts'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'culture  arts');

INSERT INTO topics (name)
SELECT 'divorce'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'divorce');

INSERT INTO topics (name)
SELECT 'education'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'education');

INSERT INTO topics (name)
SELECT 'entertainment'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'entertainment');

INSERT INTO topics (name)
SELECT 'environment'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'environment');

INSERT INTO topics (name)
SELECT 'fifty'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'fifty');

INSERT INTO topics (name)
SELECT 'food  drink'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'food  drink');

INSERT INTO topics (name)
SELECT 'good news'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'good news');

INSERT INTO topics (name)
SELECT 'green'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'green');

INSERT INTO topics (name)
SELECT 'healthy living'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'healthy living');

INSERT INTO topics (name)
SELECT 'home  living'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'home  living');

INSERT INTO topics (name)
SELECT 'impact'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'impact');

INSERT INTO topics (name)
SELECT 'latino voices'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'latino voices');

INSERT INTO topics (name)
SELECT 'media'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'media');

INSERT INTO topics (name)
SELECT 'money'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'money');

INSERT INTO topics (name)
SELECT 'parenting'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'parenting');

INSERT INTO topics (name)
SELECT 'parents'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'parents');

INSERT INTO topics (name)
SELECT 'politics'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'politics');

INSERT INTO topics (name)
SELECT 'queer voices'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'queer voices');

INSERT INTO topics (name)
SELECT 'religion'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'religion');

INSERT INTO topics (name)
SELECT 'science'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'science');

INSERT INTO topics (name)
SELECT 'sports'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'sports');

INSERT INTO topics (name)
SELECT 'style'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'style');

INSERT INTO topics (name)
SELECT 'style  beauty'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'style  beauty');

INSERT INTO topics (name)
SELECT 'taste'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'taste');

INSERT INTO topics (name)
SELECT 'tech'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'tech');

INSERT INTO topics (name)
SELECT 'the worldpost'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'the worldpost');

INSERT INTO topics (name)
SELECT 'travel'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'travel');

INSERT INTO topics (name)
SELECT 'us news'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'us news');

INSERT INTO topics (name)
SELECT 'weddings'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'weddings');

INSERT INTO topics (name)
SELECT 'weird news'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'weird news');

INSERT INTO topics (name)
SELECT 'wellness'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'wellness');

INSERT INTO topics (name)
SELECT 'women'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'women');

INSERT INTO topics (name)
SELECT 'world news'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'world news');

INSERT INTO topics (name)
SELECT 'worldpost'
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE name = 'worldpost');