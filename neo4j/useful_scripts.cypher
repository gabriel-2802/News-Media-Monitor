// Sample Cypher queries for Neo4j




// all paths between NewsSource and Article nodes
MATCH path = (s:NewsSource)-->(a:Article)
RETURN path

// Article count per NewsSource
MATCH (s:NewsSource)-[:PUBLISHED]->(a:Article)
RETURN s.name AS source, count(a) AS articles
ORDER BY articles DESC

// All articles with their properties
MATCH (a:Article) 
RETURN a.title, a.body_text, a.base_url, a.published_at, a.author;