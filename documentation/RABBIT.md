# RabbitMQ Topology

Message-queue wiring for the news-monitor pipeline: how an article moves from "just scraped" to "classified and clustered," and how the Provider notifies interested subscribers. Topology is provisioned by [`rabbitmq/setup.sh`](rabbitmq/setup.sh) (idempotent — safe to re-run via `make rabbitmq-setup`).

- **vhost:** `news_monitor`
- **Credentials / URL:** `RABBITMQ_URL` (e.g. `amqp://admin:secret@localhost:5672/news_monitor`)
- **Management UI:** `http://localhost:15672` (`make rabbitmq-url` for credentials)

## Diagram

```mermaid
flowchart LR
    scraper[scraper worker] -->|scrape.job| scrapeJobs[(scrape.jobs)]
    scrapeJobs --> scraper

    scraper -->|article.saved| topicExchange{{news_monitor<br/>topic exchange}}
    topicExchange -->|article.saved| classifyQ[(article.classify)]
    topicExchange -->|article.saved| clusterQ[(article.cluster)]

    classifyQ --> classifier[classifier worker]
    clusterQ --> clusterer[clusterer worker]

    provider[Provider service] -.->|PATCH /api/articles/topic<br/>PATCH /api/stories/attach| provider
    provider -->|default exchange,<br/>routing key = queue name| notifQ[(article.notifications)]

    scrapeJobs -.->|expired / dead-lettered| dlx{{news_monitor.dlx<br/>fanout exchange}}
    dlx --> deadQ[(scrape.jobs.dead)]

    classifier -->|PATCH /api/articles/topic| provider
    clusterer -->|PATCH /api/stories/attach<br/>POST /api/stories| provider
```

`article.notifications` is deliberately **not** attached to the topic exchange — see [Isolation of `article.notifications`](#isolation-of-articlenotifications) below.

## Exchanges

| Exchange             | Type   | Durable | Purpose                                                                                                     |
| -------------------- | ------ | ------- | ----------------------------------------------------------------------------------------------------------- |
| `news_monitor`     | topic  | yes     | Fan-out hub. Scraper publishes here; classify/cluster queues bind to it.                                    |
| `news_monitor.dlx` | fanout | yes     | Dead-letter target for`scrape.jobs` (messages that hit the 24h TTL land here, regardless of routing key). |

## Queues

| Queue                     | Durable | Extra args                                                                    | Bound to                                 | Routing key                          |
| ------------------------- | ------- | ----------------------------------------------------------------------------- | ---------------------------------------- | ------------------------------------ |
| `scrape.jobs`           | yes     | `x-dead-letter-exchange=news_monitor.dlx`, `x-message-ttl=86400000` (24h) | `news_monitor`                         | `scrape.job`                       |
| `scrape.jobs.dead`      | yes     | —                                                                            | `news_monitor.dlx`                     | `#` (catch-all)                    |
| `article.classify`      | yes     | —                                                                            | `news_monitor`                         | `article.saved`                    |
| `article.cluster`       | yes     | —                                                                            | `news_monitor`                         | `article.saved`                    |
| `article.notifications` | yes     | —                                                                            | **nothing** — no exchange binding | n/a (delivered via default exchange) |

## Producers, consumers, and message shapes

### `scrape.jobs`

- **Producers:**
  - Provider — `POST /api/articles/trigger-scrape` and the internal `ScrapeScheduler` (every 6 hours), one message per registered news source.
  - Scraper worker itself, when a job fails and hasn't hit `SCRAPE_MAX_ATTEMPTS` (re-queues with an incremented `retry_count`).
- **Consumer:** scraper worker (`workers/scraper/scraper_worker.py`, `make scrape-worker`).
- **Message:**
  ```json
  { "name": "bbc", "retry_count": 0 }
  ```

  `retry_count` is optional on publish, defaults to `0`.

### `article.classify` / `article.cluster`

- **Producer:** scraper worker — after each article is saved via the Provider, publishes once to the `news_monitor` exchange with routing key `article.saved`. The topic exchange fans that single publish out to **both** queues; classification and clustering run as independent, non-competing consumers of the same event.
- **Consumers:**
  - `article.classify` → classifier worker (`workers/classifier/classifier_worker.py`, `make classify-worker`) — classifies topic, writes it back via `PATCH /api/articles/topic`.
  - `article.cluster` → clusterer worker (`workers/clusterer/clusterer_worker.py`, `make cluster-worker`) — embeds the article, finds/creates a Story via Qdrant + the Provider, attaches via `PATCH /api/stories/{storyId}/attach`.
- **Message** (identical on both queues — it's the same publish, fanned out):
  ```json
  { "url": "https://example.com/news/...", "source_name": "bbc" }
  ```

### `article.notifications`

- **Producer:** Provider only (`ArticleService`/`StoryService`), published directly to the queue by name via RabbitMQ's default (nameless) exchange — i.e. `rabbitTemplate.convertAndSend("article.notifications", message)`. This does **not** go through the `news_monitor` topic exchange.
- **Consumer:** Manager(`ArticleNotificationListener`), creates the notifications for users.
- **Condition:** only published when a [subscription](provider/API.md#subscriptions) exists for the affected story/topic — see `StoryService.notifySubscribers` and `ArticleService.notifySubscribers`.
- **Message:**
  ```json
  { "name": "Politics", "articleUrl": "https://example.com/news/...", "type": "TOPIC" }
  ```

  `type` is `TOPIC` (from `PATCH /api/articles/topic`) or `STORY` (from `PATCH /api/stories/{storyId}/attach`); `name` is the topic name or story title respectively.

### `scrape.jobs.dead`

- **Producer:** RabbitMQ itself, via the `news_monitor.dlx` dead-letter exchange, whenever a `scrape.jobs` message expires (24h TTL) unconsumed.
- **Consumer:** none — inspection/replay only (`make rabbitmq-list-queues`, management UI).

## Environment variables

Defined in the repo-root `.env`, consumed by `workers/*_worker.py` (via `env_config.require_env`) and `provider/src/main/resources/application.yaml`.

| Variable                        | Default                                             | Used by                                                     |
| ------------------------------- | --------------------------------------------------- | ----------------------------------------------------------- |
| `RABBITMQ_URL`                | `amqp://admin:secret@localhost:5672/news_monitor` | Provider, all workers                                       |
| `NEWS_EXCHANGE`               | `news_monitor`                                    | scraper worker (publish target for both routing keys below) |
| `SCRAPE_JOBS_QUEUE`           | `scrape.jobs`                                     | Provider (produces), scraper worker (consumes + re-queues)  |
| `SCRAPE_JOB_ROUTING_KEY`      | `scrape.job`                                      | scraper worker                                              |
| `ARTICLE_SAVED_ROUTING_KEY`   | `article.saved`                                   | scraper worker                                              |
| `ARTICLE_CLASSIFY_QUEUE`      | `article.classify`                                | classifier worker (consumes)                                |
| `ARTICLE_CLUSTER_QUEUE`       | `article.cluster`                                 | clusterer worker (consumes)                                 |
| `ARTICLE_NOTIFICATIONS_QUEUE` | `article.notifications`                           | Provider (produces only; no worker consumes this)           |

## Operating

| Task                    | Command                             |
| ----------------------- | ----------------------------------- |
| (Re-)provision topology | `make rabbitmq-setup`             |
| List queues             | `make rabbitmq-list-queues`       |
| List exchanges          | `make rabbitmq-list-exchanges`    |
| List bindings           | `make rabbitmq-list-bindings`     |
| Purge`scrape.jobs`    | `make rabbitmq-purge-scrape-jobs` |
| Management UI / creds   | `make rabbitmq-url`               |
