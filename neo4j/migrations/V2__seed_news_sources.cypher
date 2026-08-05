// Every NewsSource must get an `id` here — the application's NewsSource
// entity has a generated `id` field (see NewsSource.java / UuidStringIdGenerator),
// and Spring Data Neo4j uses that field to decide whether a fetched entity
// is "new" or already persisted. A seeded node left without `id` reads back
// as id=null, so any later save that touches it (e.g. attaching an Article
// to it) gets treated as a brand-new NewsSource and collides with the
// unique constraint on `name` from V1. ON CREATE SET keeps MERGE idempotent
// (id is assigned once, not on every re-run/match).
MERGE (n:NewsSource {name: "bbc",         base_url: "https://www.bbc.co.uk",        rss_url: "https://feeds.bbci.co.uk/news/world/rss.xml"})         ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "guardian",    base_url: "https://www.theguardian.com",   rss_url: "https://www.theguardian.com/world/rss"})               ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "aljazeera",   base_url: "https://www.aljazeera.com",     rss_url: "https://www.aljazeera.com/xml/rss/all.xml"})           ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "npr",         base_url: "https://www.npr.org",           rss_url: "https://feeds.npr.org/1001/rss.xml"})                  ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "dw",          base_url: "https://www.dw.com",            rss_url: "https://rss.dw.com/rdf/rss-en-all"})                   ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "france24",    base_url: "https://www.france24.com",      rss_url: "https://www.france24.com/en/rss"})                     ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "cbc",         base_url: "https://www.cbc.ca",            rss_url: "https://www.cbc.ca/cmlink/rss-world"})                 ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "abc_au",      base_url: "https://www.abc.net.au",        rss_url: "https://www.abc.net.au/news/feed/51120/rss.xml"})      ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "euronews",    base_url: "https://www.euronews.com",      rss_url: "https://www.euronews.com/rss"})                        ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "sky",         base_url: "https://news.sky.com",          rss_url: "https://feeds.skynews.com/feeds/rss/world.xml"})       ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "independent", base_url: "https://www.independent.co.uk", rss_url: "https://www.independent.co.uk/news/world/rss"})        ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "thehill",     base_url: "https://thehill.com",           rss_url: "https://thehill.com/feed"})                            ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "vox",         base_url: "https://www.vox.com",           rss_url: "https://www.vox.com/rss/index.xml"})                   ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "time",        base_url: "https://time.com",              rss_url: "https://time.com/feed"})                               ON CREATE SET n.id = randomUUID();
MERGE (n:NewsSource {name: "rt",          base_url: "https://www.rt.com",            rss_url: "https://www.rt.com/rss/news"})                         ON CREATE SET n.id = randomUUID();
