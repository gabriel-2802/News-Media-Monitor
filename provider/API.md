# Provider API

REST API for registering news sources and ingesting/querying scraped articles. Backed by Neo4j.

- **Base URL:** `http://localhost:8080` (default; configurable via `server.port`)
- **Content type:** `application/json` for all request/response bodies
- **Interactive docs:** Swagger UI at `/swagger-ui.html`, raw OpenAPI spec at `/v3/api-docs`

## Table of contents

- [News Sources](#news-sources)
  - [`GET /api/news-sources`](#get-apinews-sources)
  - [`GET /api/news-sources/{sourceName}`](#get-apinews-sourcessourcename)
  - [`POST /api/news-sources`](#post-apinews-sources)
  - [`PATCH /api/news-sources/{sourceName}/failure`](#patch-apinews-sourcessourcenamefailure)
  - [`PATCH /api/news-sources/{sourceName}/reset`](#patch-apinews-sourcessourcenamereset)
- [Articles](#articles)
  - [`GET /api/articles`](#get-apiarticles)
  - [`GET /api/articles/source/{sourceName}`](#get-apiarticlessourcesourcename)
  - [`POST /api/articles`](#post-apiarticles)
  - [`GET /api/articles/exists`](#get-apiarticlesexists)
- [Objects](#objects)
  - [`NewsSourceDto`](#newssourcedto)
  - [`NewsSourceRequest`](#newssourcerequest)
  - [`ArticleDto`](#articledto)
  - [`ArticleRequest`](#articlerequest)
  - [`ErrorResponse`](#errorresponse)
- [Error handling](#error-handling)

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

---

### `PATCH /api/news-sources/{sourceName}/failure`

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

---

### `PATCH /api/news-sources/{sourceName}/reset`

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

### `POST /api/articles`

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

```json
{
  "name": "example-news",
  "baseUrl": "https://example.com",
  "rssUrl": "https://example.com/rss",
  "failureCount": 0,
  "disabled": false,
  "articleCount": 42
}
```

### `NewsSourceRequest`

Request body for registering a news source.

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
| source      | string        | Name of the source that published it |

```json
{
  "author": "Jane Doe",
  "title": "Senate passes new budget bill",
  "url": "https://example.com/news/senate-passes-budget",
  "bodyText": "Full article text goes here.",
  "publishedAt": "2026-07-01T14:30:00",
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
| `BusinessException`                   | 400    | Domain rule violations (duplicate source/article, mismatched URLs, unreachable URLs, unknown source) |
| Any other unhandled exception         | 500    | Generic `"Unexpected error occurred"` message, no internal details leaked |
