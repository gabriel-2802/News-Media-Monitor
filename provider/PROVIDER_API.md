# Provider API

REST API for registering news sources and ingesting/querying scraped articles, stories, topics, and subscriptions. Backed by Neo4j.

- **Base URL:** `http://localhost:8080/news-provider` (default; port via `server.port`, prefix via `server.servlet.context-path`)
- **Content type:** `application/json` for all request/response bodies
- **Interactive docs:** Swagger UI at `/news-provider/swagger-ui.html`, raw OpenAPI spec at `/news-provider/v3/api-docs`

## Table of contents

- [Authentication](#authentication)
- [News Sources](#news-sources)
  - [`GET /api/news-sources`](#get-apinews-sources)
  - [`GET /api/news-sources/{sourceName}`](#get-apinews-sourcessourcename)
  - [`POST /api/news-sources`](#post-apinews-sources)
  - [`PUT /api/news-sources/{sourceName}`](#put-apinews-sourcessourcename)
  - [`PATCH /api/news-sources/{sourceName}/failure`](#patch-apinews-sourcessourcenamefailure)
  - [`PATCH /api/news-sources/{sourceName}/reset`](#patch-apinews-sourcessourcenamereset)
- [Articles](#articles)
  - [`GET /api/articles`](#get-apiarticles)
  - [`GET /api/articles/source/{sourceName}`](#get-apiarticlessourcesourcename)
  - [`GET /api/articles/story/{storyId}`](#get-apiarticlesstorystoryid)
  - [`GET /api/articles/topic/{topicName}`](#get-apiarticlestopictopicname)
  - [`POST /api/articles`](#post-apiarticles)
  - [`GET /api/articles/exists`](#get-apiarticlesexists)
  - [`GET /api/articles/by-url`](#get-apiarticlesby-url)
  - [`PATCH /api/articles/topic`](#patch-apiarticlestopic)
  - [`POST /api/articles/trigger-scrape`](#post-apiarticlestrigger-scrape)
  - [`GET /api/articles/search`](#get-apiarticlessearch)
- [Stories](#stories)
  - [`GET /api/stories`](#get-apistories)
  - [`GET /api/stories/recent`](#get-apistoriesrecent)
  - [`POST /api/stories`](#post-apistories)
  - [`GET /api/stories/by-article`](#get-apistoriesby-article)
  - [`PATCH /api/stories/{storyId}/attach`](#patch-apistoriesstoryidattach)
  - [`GET /api/stories/exists`](#get-apistoriesexists)
  - [`GET /api/stories/search`](#get-apistoriessearch)
- [Topics](#topics)
  - [`GET /api/topics`](#get-apitopics)
  - [`GET /api/topics/exists`](#get-apitopicsexists)
- [Subscriptions](#subscriptions)
  - [`POST /api/subscriptions/story/{storyId}`](#post-apisubscriptionsstorystoryid)
  - [`DELETE /api/subscriptions/story/{storyId}`](#delete-apisubscriptionsstorystoryid)
  - [`POST /api/subscriptions/topic/{topicName}`](#post-apisubscriptionstopictopicname)
  - [`DELETE /api/subscriptions/topic/{topicName}`](#delete-apisubscriptionstopictopicname)
- [RabbitMQ side effects](#rabbitmq-side-effects)
- [Objects](#objects)
  - [`NewsSourceDto`](#newssourcedto)
  - [`NewsSourceRequest`](#newssourcerequest)
  - [`ArticleDto`](#articledto)
  - [`ArticleRequest`](#articlerequest)
  - [`TopicSetRequest`](#topicsetrequest)
  - [`StoryDto`](#storydto)
  - [`AttachArticleRequest`](#attacharticlerequest)
  - [`TopicDto`](#topicdto)
  - [`SubscriptionDto`](#subscriptiondto)
  - [`ErrorResponse`](#errorresponse)
- [Error handling](#error-handling)

---

## Authentication

All `GET` endpoints are public — no token required. Every mutating endpoint (`POST`/`PUT`/`PATCH`/`DELETE`) requires a `Bearer` JWT in the `Authorization` header, scoped to one or more roles:

```
Authorization: Bearer <token>
```

The Provider does not issue tokens itself — it only **validates** JWTs signed with the same `jwt.secret` as the [manager service](../manager/MANAGER_API.md), which is the sole issuer. Callers authenticate against the manager and reuse that token here:

- **Human callers (ROLE_ADMIN):** `POST /api/auth/login` on the manager with an admin account's email/password.
- **Service callers (ROLE_SYSTEM):** `POST /api/auth/login` on the manager with `{"systemCode": "<shared secret>"}` and no password. The manager itself generates one such system token once at startup (for its own outbound calls into `/api/subscriptions/**`), and each Python worker (`workers/provider_client.py`) does the same at construction, transparently re-logging-in if the Provider ever responds `401` (expired token).

### Required roles by endpoint

| Endpoint | Roles |
|----------|-------|
| `POST /api/news-sources` | ROLE_ADMIN |
| `PUT /api/news-sources/{sourceName}` | ROLE_ADMIN |
| `PATCH /api/news-sources/{sourceName}/failure` | ROLE_ADMIN, ROLE_SYSTEM |
| `PATCH /api/news-sources/{sourceName}/reset` | ROLE_ADMIN, ROLE_SYSTEM |
| `POST /api/articles` | ROLE_ADMIN, ROLE_SYSTEM |
| `PATCH /api/articles/topic` | ROLE_ADMIN, ROLE_SYSTEM |
| `POST /api/articles/trigger-scrape` | ROLE_ADMIN |
| `POST /api/stories` | ROLE_SYSTEM |
| `PATCH /api/stories/{storyId}/attach` | ROLE_SYSTEM |
| `POST /api/subscriptions/story/{storyId}` | ROLE_ADMIN, ROLE_SYSTEM |
| `DELETE /api/subscriptions/story/{storyId}` | ROLE_ADMIN, ROLE_SYSTEM |
| `POST /api/subscriptions/topic/{topicName}` | ROLE_ADMIN, ROLE_SYSTEM |
| `DELETE /api/subscriptions/topic/{topicName}` | ROLE_ADMIN, ROLE_SYSTEM |
| Every `GET` endpoint (all controllers) | none — public |

### Error responses

Auth failures short-circuit before reaching a controller, but still return the same [`ErrorResponse`](#errorresponse) shape as the rest of the API:

| Status | Cause | `message` |
|--------|-------|-----------|
| 401 | Missing, malformed, or expired/invalid-signature token | `"Missing or invalid authentication token."` |
| 403 | Valid token, but missing the required role | `"You do not have permission to perform this action."` |

### Trying it in Swagger UI

Swagger UI (`/news-provider/swagger-ui.html`) exposes an **Authorize** button — paste a raw token (no `Bearer ` prefix) obtained from the manager's `/api/auth/login` to exercise protected endpoints via "Try it out".

---

## News Sources

Base path: `/api/news-sources`

### `GET /api/news-sources`

List all registered news sources, paginated, including each source's article count.

**Query parameters**

| Name  | Type | Default | Description               |
|-------|------|---------|----------------------------|
| page  | int  | `0`     | Zero-based page index      |
| count | int  | `20`    | Number of sources per page |

**Responses**

| Status | Body                        | Description                |
|--------|-----------------------------|-----------------------------|
| 200    | [`NewsSourceDto[]`](#newssourcedto) | Sources retrieved successfully |

---

### `GET /api/news-sources/{sourceName}`

Get a single registered news source by name, including its article count.

**Path parameters**

| Name       | Type   | Description                    |
|------------|--------|----------------------------------|
| sourceName | string | Name of the news source, e.g. `example-news` |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`NewsSourceDto`](#newssourcedto) | News source retrieved successfully |
| 400 | [`ErrorResponse`](#errorresponse) | News source does not exist |

---

### `POST /api/news-sources`

**Requires:** `ROLE_ADMIN`

Register a new news source to be scraped. The service validates the source before saving:

1. Fails if a source with the same `baseUrl`, `name`, or `rssUrl` already exists.
2. Fails if `rssUrl`'s domain doesn't match `baseUrl`'s domain.
3. Fails if either `baseUrl` or `rssUrl` is unreachable (server performs a live HTTP reachability check).

**Request body:** [`NewsSourceRequest`](#newssourcerequest)

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 201 | [`NewsSourceDto`](#newssourcedto) | News source created successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Validation failed, a source with the same name/baseUrl/rssUrl already exists, domains don't match, or the URLs are unreachable |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN` |

---

### `PUT /api/news-sources/{sourceName}`

**Requires:** `ROLE_ADMIN`

Replaces a news source's data with the desired final state. Fails if the new name/baseUrl/rssUrl already belongs to a different source, or if either URL is unreachable.

**Path parameters**

| Name       | Type   | Description                    |
|------------|--------|----------------------------------|
| sourceName | string | Name of the news source, e.g. `example-news` |

**Request body:** [`NewsSourceRequest`](#newssourcerequest)

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`NewsSourceDto`](#newssourcedto) | News source updated successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Validation failed, the source does not exist, the new name/baseUrl/rssUrl already belongs to a different source, or the URLs are unreachable |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN` |

---

### `PATCH /api/news-sources/{sourceName}/failure`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Increments the consecutive scrape-failure counter for the given source. Used by the scraper to track sources that repeatedly fail to be reached. If the incremented count reaches **3**, the source is automatically disabled (`isDisabled = true`).

**Path parameters**

| Name       | Type   | Description                    |
|------------|--------|----------------------------------|
| sourceName | string | Name of the news source, e.g. `example-news` |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`NewsSourceDto`](#newssourcedto) | Failure count incremented successfully |
| 400 | [`ErrorResponse`](#errorresponse) | News source does not exist |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

### `PATCH /api/news-sources/{sourceName}/reset`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Resets a source's failure counter to `0` and re-enables it (`isDisabled = false`) if it had been disabled.

**Path parameters**

| Name       | Type   | Description                    |
|------------|--------|----------------------------------|
| sourceName | string | Name of the news source, e.g. `example-news` |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`NewsSourceDto`](#newssourcedto) | Failure count reset successfully |
| 400 | [`ErrorResponse`](#errorresponse) | News source does not exist |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

## Articles

Base path: `/api/articles`

### `GET /api/articles`

List all articles across all sources, paginated.

**Query parameters**

| Name  | Type | Default | Description                |
|-------|------|---------|------------------------------|
| page  | int  | `0`     | Zero-based page index       |
| count | int  | `20`    | Number of articles per page |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`ArticleDto[]`](#articledto) | Articles retrieved successfully |

---

### `GET /api/articles/source/{sourceName}`

List articles published by a specific news source, paginated.

**Path parameters**

| Name       | Type   | Description             |
|------------|--------|---------------------------|
| sourceName | string | Name of the news source |

**Query parameters**

| Name  | Type | Default | Description                |
|-------|------|---------|------------------------------|
| page  | int  | `0`     | Zero-based page index       |
| count | int  | `20`    | Number of articles per page |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`ArticleDto[]`](#articledto) | Articles retrieved successfully |

---

### `GET /api/articles/story/{storyId}`

List articles that belong to a given story cluster, paginated.

**Path parameters**

| Name    | Type   | Description             |
|---------|--------|---------------------------|
| storyId | string | ID of the story cluster |

**Query parameters**

| Name  | Type | Default | Description                |
|-------|------|---------|------------------------------|
| page  | int  | `0`     | Zero-based page index       |
| count | int  | `20`    | Number of articles per page |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`ArticleDto[]`](#articledto) | Articles retrieved successfully |

---

### `GET /api/articles/topic/{topicName}`

List articles tagged with a given topic, paginated.

**Path parameters**

| Name      | Type   | Description       |
|-----------|--------|---------------------|
| topicName | string | Name of the topic, e.g. `politics` |

**Query parameters**

| Name  | Type | Default | Description                |
|-------|------|---------|------------------------------|
| page  | int  | `0`     | Zero-based page index       |
| count | int  | `20`    | Number of articles per page |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`ArticleDto[]`](#articledto) | Articles retrieved successfully |

---

### `POST /api/articles`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Persist a scraped article. Validated before saving:

1. Fails if an article with the same `url` already exists.
2. Fails if `sourceName` does not reference a registered news source.
3. Fails if `url` does not belong to the referenced source's `baseUrl`.

**Request body:** [`ArticleRequest`](#articlerequest)

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 201 | [`ArticleDto`](#articledto) | Article created successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Validation failed, article URL already exists, source does not exist, or the URL does not match the source's base URL |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

### `GET /api/articles/exists`

Check whether an article with the given URL has already been ingested. Used by the scraper to skip already-seen articles.

**Query parameters**

| Name | Type   | Description             |
|------|--------|---------------------------|
| url  | string | Canonical URL to check, e.g. `https://example.com/news/senate-passes-budget` |

**Responses**

| Status | Body      | Description           |
|--------|-----------|--------------------------|
| 200    | `boolean` | Existence check result |

---

### `GET /api/articles/by-url`

Get a single article by its canonical URL.

**Query parameters**

| Name | Type   | Description             |
|------|--------|---------------------------|
| url  | string | Canonical URL of the article |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`ArticleDto`](#articledto) | Article retrieved successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Article does not exist |

---

### `PATCH /api/articles/topic`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Tags an article with a topic, replacing any topic it already had. If a [subscription](#subscriptions) exists for the topic, publishes a notification message — see [RabbitMQ side effects](#rabbitmq-side-effects).

**Request body:** [`TopicSetRequest`](#topicsetrequest)

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`ArticleDto`](#articledto) | Topic set successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Article does not exist |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

### `POST /api/articles/trigger-scrape`

**Requires:** `ROLE_ADMIN`

Publishes a scrape job for every registered news source onto the `scrape.jobs` queue, to be picked up by the scraper worker. Also runs automatically every 6 hours (see [`ScrapeScheduler`](#rabbitmq-side-effects)).

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 202 | `integer` | Number of scrape jobs queued |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN` |

---

### `GET /api/articles/search`

Full-text search over article titles and body text, ordered by relevance. Matching is prefix-based per term (e.g. `"sen bud"` matches text containing words starting with `sen` and `bud`).

**Query parameters**

| Name  | Type   | Default | Description                |
|-------|--------|---------|------------------------------|
| q     | string | —       | Free-text search query (required) |
| page  | int    | `0`     | Zero-based page index       |
| count | int    | `20`    | Number of articles per page |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`ArticleDto[]`](#articledto) | Matching articles retrieved successfully |

---

## Stories

Base path: `/api/stories`

### `GET /api/stories`

List all story clusters, paginated, ordered by most recently updated. Each story is hydrated with (up to 100) of its articles.

**Query parameters**

| Name  | Type | Default | Description               |
|-------|------|---------|-----------------------------|
| page  | int  | `0`     | Zero-based page index      |
| count | int  | `20`    | Number of stories per page |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`StoryDto[]`](#storydto) | Stories retrieved successfully |

---

### `GET /api/stories/recent`

List stories that had activity within the last N days. Used to identify live clustering candidates.

**Query parameters**

| Name | Type | Default | Description                  |
|------|------|---------|---------------------------------|
| days | int  | `7`     | Look-back window in days     |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`StoryDto[]`](#storydto) | Recent stories retrieved successfully |

---

### `POST /api/stories`

**Requires:** `ROLE_SYSTEM`

Creates a new, empty story cluster with the given title.

**Query parameters**

| Name  | Type   | Description                     |
|-------|--------|------------------------------------|
| title | string | Title for the new story cluster, e.g. `Senate passes new budget bill` |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 201 | [`StoryDto`](#storydto) | Story created successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Title is blank |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_SYSTEM` |

---

### `GET /api/stories/by-article`

Returns the story cluster that the article at the given URL is attached to.

**Query parameters**

| Name | Type   | Description             |
|------|--------|---------------------------|
| url  | string | Canonical URL of the article |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`StoryDto`](#storydto) | Story retrieved successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Article does not exist, or is not attached to any story |

---

### `PATCH /api/stories/{storyId}/attach`

**Requires:** `ROLE_SYSTEM`

Creates a `BELONGS_TO` edge from the article to the story. Increments `articleCount` unconditionally, and `sourceCount` only if the article's source hasn't previously contributed to this story. Idempotent — re-attaching the same article has no effect. If a [subscription](#subscriptions) exists for the story, publishes a notification message — see [RabbitMQ side effects](#rabbitmq-side-effects).

**Path parameters**

| Name    | Type   | Description             |
|---------|--------|---------------------------|
| storyId | string | ID of the target story  |

**Request body:** [`AttachArticleRequest`](#attacharticlerequest)

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`StoryDto`](#storydto) | Article attached successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Story or article does not exist |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_SYSTEM` |

---

### `GET /api/stories/exists`

Check whether a story with the given ID exists.

**Query parameters**

| Name    | Type   | Description             |
|---------|--------|---------------------------|
| storyId | string | ID of the story to check |

**Responses**

| Status | Body      | Description             |
|--------|-----------|----------------------------|
| 200    | `boolean` | Existence check result |

---

### `GET /api/stories/search`

Full-text search over story titles, ordered by relevance. Matching is prefix-based per term.

**Query parameters**

| Name  | Type   | Default | Description                |
|-------|--------|---------|------------------------------|
| q     | string | —       | Free-text search query (required) |
| page  | int    | `0`     | Zero-based page index       |
| count | int    | `20`    | Number of stories per page  |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`StoryDto[]`](#storydto) | Matching stories retrieved successfully |

---

## Topics

Base path: `/api/topics`

### `GET /api/topics`

List every topic that has been assigned to at least one article, along with its article count.

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`TopicDto[]`](#topicdto) | Topics retrieved successfully |

---

### `GET /api/topics/exists`

Check whether a topic with the given name already exists.

**Query parameters**

| Name | Type   | Description                     |
|------|--------|------------------------------------|
| name | string | Name of the topic to check, e.g. `Politics` |

**Responses**

| Status | Body      | Description             |
|--------|-----------|----------------------------|
| 200    | `boolean` | Existence check result |

---

## Subscriptions

Base path: `/api/subscriptions`

A subscription tracks interest in a story or a topic via a `count` — each subscribe call creates it at `count = 1` or increments an existing one; each unsubscribe call decrements it, deleting the subscription once `count` reaches `0`. Modeled as a `Subscription` node with a `SUBSCRIBES_TO` relationship to either a `Story` or a `Topic` node.

Every endpoint in this controller requires `ROLE_ADMIN` or `ROLE_SYSTEM`.

### `POST /api/subscriptions/story/{storyId}`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Creates a subscription to the given story with `count = 1`, or increments the count if one already exists.

**Path parameters**

| Name    | Type   | Description                    |
|---------|--------|-----------------------------------|
| storyId | string | ID of the story to subscribe to |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`SubscriptionDto`](#subscriptiondto) | Subscribed successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Story does not exist |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

### `DELETE /api/subscriptions/story/{storyId}`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Decrements the subscription count for the given story. The subscription is deleted once the count reaches `0`.

**Path parameters**

| Name    | Type   | Description                       |
|---------|--------|--------------------------------------|
| storyId | string | ID of the story to unsubscribe from |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 204 | — | Unsubscribed successfully |
| 400 | [`ErrorResponse`](#errorresponse) | No subscription exists for this story |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

### `POST /api/subscriptions/topic/{topicName}`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Creates a subscription to the given topic with `count = 1`, or increments the count if one already exists.

**Path parameters**

| Name      | Type   | Description                       |
|-----------|--------|---------------------------------------|
| topicName | string | Name of the topic to subscribe to, e.g. `Politics` |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 200 | [`SubscriptionDto`](#subscriptiondto) | Subscribed successfully |
| 400 | [`ErrorResponse`](#errorresponse) | Topic does not exist |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

### `DELETE /api/subscriptions/topic/{topicName}`

**Requires:** `ROLE_ADMIN` or `ROLE_SYSTEM`

Decrements the subscription count for the given topic. The subscription is deleted once the count reaches `0`.

**Path parameters**

| Name      | Type   | Description                          |
|-----------|--------|------------------------------------------|
| topicName | string | Name of the topic to unsubscribe from |

**Responses**

| Status | Body | Description |
|--------|------|--------------|
| 204 | — | Unsubscribed successfully |
| 400 | [`ErrorResponse`](#errorresponse) | No subscription exists for this topic |
| 401 | [`ErrorResponse`](#errorresponse) | Missing/invalid token — see [Authentication](#authentication) |
| 403 | [`ErrorResponse`](#errorresponse) | Token lacks `ROLE_ADMIN`/`ROLE_SYSTEM` |

---

## RabbitMQ side effects

The Provider is a RabbitMQ producer only — it never consumes. Full topology, queue names, and consumers are documented in [`../RABBIT.md`](../RABBIT.md). Summary of what the HTTP API above triggers:

| Endpoint | Condition | Queue | Message |
|----------|-----------|-------|---------|
| `POST /api/articles/trigger-scrape` (and the internal 6-hourly `ScrapeScheduler`) | always, once per registered news source | `scrape.jobs` | `{"name": "<sourceName>", "retry_count": 0}` |
| `PATCH /api/articles/topic` | only if a subscription exists for the topic | `article.notifications` | `{"name": "<topicName>", "articleUrl": "<url>", "type": "TOPIC"}` |
| `PATCH /api/stories/{storyId}/attach` | only if a subscription exists for the story | `article.notifications` | `{"name": "<story title>", "articleUrl": "<url>", "type": "STORY"}` |

---

## Objects

### `NewsSourceDto`

Response shape for a news source.

| Field         | Type    | Description                                    |
|---------------|---------|-------------------------------------------------|
| name          | string  | Source's unique name                            |
| baseUrl       | string  | Source's homepage/base URL                      |
| rssUrl        | string  | Source's RSS feed URL                           |
| failureCount  | int     | Consecutive scrape-failure count                |
| disabled      | boolean | Whether the source is currently disabled        |
| articleCount  | long    | Total number of articles ingested from this source |
| notes         | string  | Free-text notes about this source               |
| politicalView | string  | Political leaning/bias classification, e.g. `center-left` |
| sources       | string[]| Reference sources/citations backing the bias classification |
| biasScores    | map<string,string> | Bias scores keyed by rating provider, e.g. `{"AllSides": "Lean Left"}` |

```json
{
  "name": "example-news",
  "baseUrl": "https://example.com",
  "rssUrl": "https://example.com/rss",
  "failureCount": 0,
  "disabled": false,
  "articleCount": 42,
  "notes": null,
  "politicalView": "center-left",
  "sources": [],
  "biasScores": {}
}
```

### `NewsSourceRequest`

Request body for registering/updating a news source.

| Field   | Type   | Required | Constraints        |
|---------|--------|----------|----------------------|
| name    | string | yes      | not null, not empty |
| baseUrl | string | yes      | not null, not empty |
| rssUrl  | string | yes      | not null, not empty |

```json
{
  "name": "example-news",
  "baseUrl": "https://example.com",
  "rssUrl": "https://example.com/rss"
}
```

### `ArticleDto`

Response shape for an article.

| Field       | Type          | Description                        |
|-------------|---------------|--------------------------------------|
| author      | string        | Article author (may be null/blank) |
| title       | string        | Article title                       |
| url         | string        | Canonical article URL               |
| bodyText    | string        | Full article body text              |
| publishedAt | datetime      | ISO-8601 local date-time the article was published |
| topic       | string        | Name of the article's topic, if tagged (may be null) |
| source      | string        | Name of the source that published it |

```json
{
  "author": "Jane Doe",
  "title": "Senate passes new budget bill",
  "url": "https://example.com/news/senate-passes-budget",
  "bodyText": "Full article text goes here.",
  "publishedAt": "2026-07-01T14:30:00",
  "topic": "politics",
  "source": "example-news"
}
```

### `ArticleRequest`

Request body for saving an article.

| Field       | Type     | Required | Constraints          |
|-------------|----------|----------|------------------------|
| author      | string   | no       | —                     |
| title       | string   | yes      | not null, not empty  |
| url         | string   | yes      | not null, not empty; must belong to `sourceName`'s baseUrl |
| bodyText    | string   | yes      | not null, not empty  |
| publishedAt | datetime | yes      | not null (ISO-8601 local date-time) |
| sourceName  | string   | yes      | not null, not empty; must reference an existing news source |

```json
{
  "author": "Jane Doe",
  "title": "Senate passes new budget bill",
  "url": "https://example.com/news/senate-passes-budget",
  "bodyText": "Full article text goes here.",
  "publishedAt": "2026-07-01T14:30:00",
  "sourceName": "example-news"
}
```

### `TopicSetRequest`

Request body for tagging an article with a topic.

| Field | Type   | Required | Constraints          |
|-------|--------|----------|------------------------|
| url   | string | yes      | not null, not empty; article must exist |
| topic | string | yes      | not null, not empty  |

```json
{
  "url": "https://www.bbc.co.uk/news/articles/ckg8m2xkg84o",
  "topic": "religion"
}
```

### `StoryDto`

Response shape for a story cluster.

| Field         | Type              | Description                                    |
|---------------|-------------------|-------------------------------------------------|
| id            | string            | Unique identifier of the story                 |
| title         | string            | Title derived from the first attached article  |
| createdAt     | datetime          | When the story was first created               |
| lastUpdated   | datetime          | When the last article was attached             |
| articleCount  | int               | Total number of articles attached to this story |
| sourceCount   | int               | Number of distinct sources that contributed     |
| trendingScore | double            | Precomputed trending score (recency + velocity + source diversity) |
| articles      | [`ArticleDto[]`](#articledto) | Attached articles; empty unless explicitly populated (e.g. by `GET /api/stories`) |

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Senate passes new budget bill",
  "createdAt": "2026-07-01T14:00:00Z",
  "lastUpdated": "2026-07-01T18:30:00Z",
  "articleCount": 12,
  "sourceCount": 5,
  "trendingScore": 3.14,
  "articles": []
}
```

### `AttachArticleRequest`

Request body for attaching an article to a story.

| Field      | Type   | Required | Constraints                       |
|------------|--------|----------|--------------------------------------|
| articleUrl | string | yes      | not null, not empty; article must exist |

```json
{
  "articleUrl": "https://example.com/news/senate-passes-budget"
}
```

### `TopicDto`

Response shape for a topic.

| Field        | Type   | Description                                       |
|--------------|--------|-----------------------------------------------------|
| name         | string | Topic name                                          |
| articleCount | long   | Number of articles currently tagged with this topic |

```json
{
  "name": "Politics",
  "articleCount": 17
}
```

### `SubscriptionDto`

Response shape for a subscription. Exactly one of `storyId`/`topicName` is populated, depending on what was subscribed to.

| Field     | Type   | Description                                              |
|-----------|--------|--------------------------------------------------------------|
| id        | string | Subscription ID                                          |
| storyId   | string | ID of the subscribed story, if this is a story subscription (else `null`) |
| topicName | string | Name of the subscribed topic, if this is a topic subscription (else `null`) |
| count     | int    | Number of active subscriptions                            |

```json
{
  "id": "3f1b2c3d-4e5f-6789-0abc-def123456789",
  "storyId": null,
  "topicName": "Politics",
  "count": 1
}
```

### `ErrorResponse`

Standard error shape returned by every non-2xx response.

| Field     | Type              | Description                                  |
|-----------|-------------------|-------------------------------------------------|
| timestamp | datetime          | When the error occurred                       |
| status    | int               | HTTP status code                              |
| error     | string            | HTTP status reason phrase                     |
| message   | string            | Human-readable error message                  |
| details   | map<string,string>| Field-level validation errors, if any (empty map otherwise) |

```json
{
  "timestamp": "2026-07-03T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "title": "must not be empty",
    "url": "must not be empty"
  }
}
```

---

## Error handling

All exceptions are mapped to an [`ErrorResponse`](#errorresponse) by a global exception handler:

| Exception                            | Status | Notes                                             |
|---------------------------------------|--------|-----------------------------------------------------|
| `MethodArgumentNotValidException`     | 400    | `@Valid @RequestBody` failures — `details` populated with per-field messages |
| `ConstraintViolationException`        | 400    | `@RequestParam`/`@PathVariable` validation failures |
| `ResponseStatusException`             | (as thrown) | Explicit `throw new ResponseStatusException(...)` |
| `BusinessException`                   | 400    | Domain rule violations (duplicate source/article, mismatched URLs, unreachable URLs, unknown source/story/topic, subscription not found) |
| Any other unhandled exception         | 500    | Generic `"Unexpected error occurred"` message, no internal details leaked |

Authentication/authorization failures don't go through this handler (Spring Security intercepts before the controller layer), but still produce the same `ErrorResponse` shape — see [Authentication](#authentication) for the 401/403 cases.
