
# News Media Monitoring — Clustering Architecture Plan

## 1. Goal

Extend the existing scraper → RabbitMQ → Neo4j pipeline so that:

- Articles covering the same real-world event are automatically grouped into **Stories**.
- End users see a feed of Stories (ranked by trending signals).
- Tapping a Story shows every source's report on that same event.

## 2. Current State

```
Source-pusher → RabbitMQ (source queue) → Scraper → Neo4j
                                                (:Article)-[:FROM_SOURCE]->(:Source)
```

This stays untouched. Clustering is added as a **new stage after ingestion**, not inside the scraper.

## 3. Target Architecture

```
Source-pusher → RabbitMQ (source queue) → Scraper → Neo4j (Article, Source)
                                              │
                                              ▼
                                   RabbitMQ (clustering queue: article_id)
                                              │
                                              ▼
                                   Clustering Worker
                                       │         │
                                       ▼         ▼
                              Vector DB      Neo4j (Story nodes,
                              (embeddings,   BELONGS_TO edges,
                              centroids)     Story metadata)
                                              │
                                              ▼
                                   Feed API → Mobile/Web App
                                              │
                          (periodic, hourly) │
                                              ▼
                              Batch Reconciliation Job (GDS / vector DB clustering)
```

### System responsibilities

| System                         | Owns                                                                                                           |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------- |
| Neo4j                          | Source of truth: Article, Source, Story nodes; relationships; metadata (counts, timestamps)                    |
| Vector DB (Qdrant recommended) | Embeddings only — article vectors and story centroid vectors, filterable by metadata                          |
| RabbitMQ                       | Two queues: existing source queue, new clustering queue                                                        |
| Clustering Worker              | New service; consumes clustering queue, computes embeddings, decides cluster assignment, writes to both stores |
| Feed API                       | New service (or endpoint set); reads Neo4j for ranked Stories and grouped Articles                             |

## 4. Data Model (Neo4j)

```
(:Source {id, name, domain, ...})

(:Article {
  id,
  url,
  title,
  body,
  publishedAt,
  scrapedAt
})

(:Story {
  id,
  createdAt,
  lastUpdated,
  articleCount,
  sourceCount,
  trendingScore
})

(:Article)-[:FROM_SOURCE]->(:Source)
(:Article)-[:BELONGS_TO]->(:Story)
(:Article)-[:DUPLICATE_OF]->(:Article)   // optional, see §8
```

No embeddings stored in Neo4j — only in the vector DB, linked by matching `id`.

## 5. Data Model (Vector DB — Qdrant)

Two separate collections (avoid cross-matching bugs, allow independent tuning):

**`article_vectors`**

```json
{ "id": "<article_id>", "vector": [...], "payload": { "publishedAt": "...", "storyId": "..." } }
```

**`story_centroids`**

```json
{ "id": "<story_id>", "vector": [...], "payload": { "publishedAt": "createdAt", "lastUpdated": "...", "articleCount": N } }
```

## 6. Article Lifecycle (step by step)

1. **Source dispatched** — unchanged, existing queue.
2. **Scraper runs** — unchanged, creates `Article`, links `FROM_SOURCE`.
3. **Scraper publishes** `{article_id}` to new `clustering-queue`.
4. **Clustering worker consumes** the message:
   a. Fetch article text from Neo4j.
   b. Generate embedding (embedding model/API call — the slow step).
   c. Optionally extract entities/keywords as a secondary signal.
   d. Upsert into `article_vectors` in the vector DB.
5. **Find candidate Story**: query `story_centroids`, filtered to `lastUpdated > now - N days`, top-k by cosine similarity.
6. **Decide**:
   - No candidates, or best score < threshold → **create new Story**:
     - Neo4j: create `Story` node, `BELONGS_TO` edge.
     - Vector DB: upsert new point in `story_centroids` with this article's vector.
   - Best score ≥ threshold → **attach to existing Story**:
     - Neo4j: create `BELONGS_TO` edge.
     - Vector DB: update that centroid (recomputed running average).
7. **Update Story metadata** in Neo4j: `articleCount`, `sourceCount` (only increment if this is a new source for the story), `lastUpdated`, recompute `trendingScore`.
8. **Feed reads** Neo4j directly — no vector DB involvement at read time.

### First articles (bootstrap)

No special-casing needed. Step 5 on an empty `story_centroids` collection just returns zero results, which is handled by the same "no match" branch as any low-similarity case → new Story is created. This is true for article #1 and for any article that starts a new story later.

## 7. Consistency Strategy

Two systems (Neo4j, vector DB) must agree per Story. To avoid orphaned state on partial failure:

- Write to the **vector DB first** (idempotent upsert by ID, cheap to retry/leave orphaned).
- Then write to **Neo4j**.
- If the Neo4j write fails, **don't ack the RabbitMQ message** — let it redeliver. The whole worker step re-runs; the vector DB upsert is idempotent (same ID overwrites), so no duplication risk.
- Only ack the clustering queue message after both writes succeed.

## 8. Near-duplicate vs. distinct-report distinction

Wire stories (AP/Reuters syndication) will otherwise show up as 5 "different reports" that are actually identical text.

- Use a **tighter similarity threshold** to flag near-duplicates.
- Model as `(:Article)-[:DUPLICATE_OF]->(:Article)` pointing to the canonical version, or a `canonicalArticleId` property.
- Feed UI: when showing "different reports" under a Story, collapse duplicates into one card with a "+N identical" indicator, so genuinely distinct coverage is what surfaces.

## 9. Batch Reconciliation (hourly job)

Online/incremental clustering will occasionally over-split or under-split. Run a periodic job (not per-article):

- Pull recent Stories (e.g. last 48h) and their centroids.
- Run clustering over that reduced set — either Neo4j GDS (Louvain / weakly-connected-components on a similarity graph) or a clustering pass in the vector DB.
- Merge Stories that should be one: reassign `BELONGS_TO` edges, merge metadata, delete the redundant Story node and centroid.
- Optionally split Stories that drifted too far from their own centroid (rare, lower priority for v1).

## 10. Feed API

- `GET /feed` — Story nodes ordered by `trendingScore` (function of recency, article velocity, distinct source count), paginated.
- `GET /feed/story/{id}` — all Articles under that Story, grouped by Source, duplicates collapsed per §8.

## 11. Threshold Tuning

The similarity threshold in step 6 is the main knob:

- Too loose → unrelated stories merge into one blob.
- Too tight → every article spawns its own Story, nothing clusters.

Approach: pull a sample of real article pairs (known-same-story and known-different-story), compute cosine similarities, and pick a threshold empirically — don't guess a number up front. Expect to revisit this after the system runs on real traffic for a week or two.

## 12. Build Order (suggested phases)

1. **Phase 1** — Stand up clustering queue + worker skeleton; wire scraper to publish article IDs (no clustering logic yet, just plumbing + logging).
2. **Phase 2** — Add embedding generation, vector DB collections, and the create-Story-if-none-exists path. Validate on a small batch of real articles.
3. **Phase 3** — Add attach-to-existing-Story path with threshold-based decision. Tune threshold on sample data.
4. **Phase 4** — Build Feed API (`/feed`, `/feed/story/{id}`) and trending score.
5. **Phase 5** — Add duplicate detection (`DUPLICATE_OF`).
6. **Phase 6** — Add hourly batch reconciliation job (GDS or vector DB clustering).

## 13. Open Decisions to Confirm

- Vector DB choice: Qdrant (self-hosted, good filtering, no vendor lock-in) vs. Pinecone (managed, less ops) vs. Weaviate (hybrid search).
- Embedding model/provider and dimensionality.
- Recency window size for candidate Story search (draft: 3–5 days, adjustable by news category — breaking news vs. slow-moving stories may need different windows).
- Trending score formula weights (recency vs. velocity vs. source diversity).
