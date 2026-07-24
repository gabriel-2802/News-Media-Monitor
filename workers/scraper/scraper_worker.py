"""
RabbitMQ-driven scrape worker.

Neo4j is never touched directly — every read/write of source or article data
goes through the Provider service's REST API (see provider/API.md).

Consumes scrape job messages from the `scrape.jobs` queue. Each job names a
source only; the worker fetches that source's config (base URL, RSS URL,
disabled flag) from the Provider, scrapes it, and:

  - If the source is disabled, it gets exactly one attempt. Failure or
    success, the message is not retried — a disabled source stays disabled
    until an operator (or a lucky attempt here) resets it.
  - If the source is enabled, a failed attempt increments its failure count
    via the Provider and the job is re-queued, up to MAX_ATTEMPTS total.
  - Every article saved through the Provider has its URL published (fanned
    out via the `article.saved` routing key) to both the `article.classify`
    and `article.cluster` queues, for the downstream classifier and
    clustering workers respectively.

Scrape job message format (JSON):
    {
        "name": "bbc",
        "retry_count": 0          # optional, defaults to 0
    }

Article-saved message format (JSON):
    {
        "url": "...",
        "source_name": "bbc"
    }

Run (from workers/, the parent of this file):
    make scrape-worker
    # or: scraper/venv/bin/python -m scraper.scraper_worker

Environment variables:
    RABBITMQ_URL     amqp://user:pass@host:port/vhost
    PROVIDER_URL     http://host:port
"""
from __future__ import annotations

import json
import logging
import time
from datetime import datetime, timezone
from typing import Any

import pika
import pika.channel
import pika.exceptions
import pika.spec

from scraper.news_scraper import (
    Article,
    ExtractionError,
    FetchError,
    HttpFetcher,
    PlaywrightFetcher,
    SmartFetcher,
    TrafilaturaExtractor,
    BROWSER_UA,
    RATE_LIMIT_SECONDS,
    USER_AGENT,
    parse_rss,
)
from env_config import require_env, require_int
from log_config import configure_logging
from provider_client import ProviderClient, ProviderError, SourceInfo

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

RABBITMQ_URL = require_env("RABBITMQ_URL")
PROVIDER_URL = require_env("PROVIDER_URL")

SCRAPE_JOBS_QUEUE        = require_env("SCRAPE_JOBS_QUEUE")
NEWS_EXCHANGE            = require_env("NEWS_EXCHANGE")
SCRAPE_JOB_ROUTING_KEY   = require_env("SCRAPE_JOB_ROUTING_KEY")
ARTICLE_SAVED_ROUTING_KEY = require_env("ARTICLE_SAVED_ROUTING_KEY")

# Matches the Provider's own auto-disable threshold (see NewsSourceRepository.incrementFailureCount).
MAX_ATTEMPTS = require_int("SCRAPE_MAX_ATTEMPTS")

RSS_BROWSER_UA_SOURCES: frozenset[str] = frozenset(
    name.strip() for name in require_env("RSS_BROWSER_UA_SOURCES").split(",") if name.strip()
)

configure_logging()
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Job handler
# ---------------------------------------------------------------------------


class JobHandler:
    def __init__(
        self,
        channel: pika.channel.Channel,
        provider: ProviderClient,
        max_attempts: int = MAX_ATTEMPTS,
        rate_limit: float = RATE_LIMIT_SECONDS,
    ) -> None:
        self._channel         = channel
        self._provider        = provider
        self._extractor       = TrafilaturaExtractor()
        self._max_attempts    = max_attempts
        self._rate_limit      = rate_limit
        # Reused across all jobs — one session, one browser pool
        self._feed_http       = HttpFetcher(user_agent=USER_AGENT)
        self._feed_http_bot   = HttpFetcher(user_agent=BROWSER_UA)
        self._article_fetcher = SmartFetcher(
            http=HttpFetcher(user_agent=USER_AGENT),
            browser=PlaywrightFetcher(),
        )

    def handle(self, method: pika.spec.Basic.Deliver, body: bytes) -> None:
        try:
            payload      = json.loads(body)
            source_name: str = payload["name"]
            retry_count: int = int(payload.get("retry_count", 0))
        except (json.JSONDecodeError, KeyError, ValueError) as exc:
            log.error("Malformed message, discarding: %r — %s", body, exc)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        log.info("[%s] Scrape job (attempt %d/%d)", source_name, retry_count + 1, self._max_attempts)

        source = self._provider.get_source(source_name)
        if source is None:
            log.warning("[%s] Unknown source (not registered with Provider) — dropping", source_name)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        if source.disabled:
            log.warning("[%s] Source is disabled — single attempt, no re-queue", source_name)
            if self._scrape_source(source):
                self._provider.reset_failures(source_name)
                log.info("[%s] Disabled source succeeded — re-enabled", source_name)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        if self._scrape_source(source):
            self._provider.reset_failures(source_name)
        else:
            disabled_now = self._provider.record_failure(source_name)
            next_attempt = retry_count + 1
            if disabled_now:
                log.error("[%s] Disabled by Provider after this failure", source_name)
            elif next_attempt < self._max_attempts:
                log.warning("[%s] Attempt %d/%d failed — re-queuing", source_name, next_attempt, self._max_attempts)
                self._publish(SCRAPE_JOB_ROUTING_KEY, {"name": source_name, "retry_count": next_attempt})
            else:
                log.error("[%s] Exhausted %d attempts — dropping", source_name, self._max_attempts)

        self._channel.basic_ack(delivery_tag=method.delivery_tag)

    # ------------------------------------------------------------------

    def _scrape_source(self, source: SourceInfo) -> bool:
        """Fetch the RSS feed and every new article for a source.

        Returns False only when the feed itself couldn't be fetched/parsed —
        individual article failures are logged and skipped, not treated as a
        source-level failure.
        """
        feed_fetcher = self._feed_http_bot if source.name in RSS_BROWSER_UA_SOURCES else self._feed_http

        try:
            feed_html = feed_fetcher.fetch(source.rss_url)
            entries   = parse_rss(feed_html)
        except FetchError as exc:
            log.warning("[%s] RSS fetch failed: %s", source.name, exc)
            return False
        except Exception as exc:
            log.warning("[%s] RSS parse failed: %s", source.name, exc)
            return False

        if not entries:
            log.warning("[%s] RSS returned no entries", source.name)
            return True

        time.sleep(self._rate_limit)
        saved = skipped = failed = 0

        for entry in entries:
            if self._provider.article_exists(entry.url):
                skipped += 1
                continue

            try:
                html      = self._article_fetcher.fetch(entry.url)
                body_text = self._extractor.extract(html, entry.url)
            except (FetchError, ExtractionError) as exc:
                log.warning("[%s] Article skipped (%s): %s", source.name, entry.url, exc)
                failed += 1
                time.sleep(self._rate_limit)
                continue

            article = Article(
                url=entry.url,
                title=entry.title,
                author=entry.author,
                published_at=entry.published_at,
                body_text=body_text,
                scraped_at=datetime.now(timezone.utc),
                source=source.name,
            )

            if not self._provider.save_article(article):
                failed += 1
                time.sleep(self._rate_limit)
                continue

            self._publish(ARTICLE_SAVED_ROUTING_KEY, {"url": article.url, "source_name": article.source})
            saved += 1
            log.info("[%s] Saved + queued for clustering: %s", source.name, article.title[:80])
            time.sleep(self._rate_limit)

        log.info("[%s] Done — saved=%d skipped=%d failed=%d", source.name, saved, skipped, failed)
        return True

    def _publish(self, routing_key: str, payload: dict[str, Any]) -> None:
        self._channel.basic_publish(
            exchange=NEWS_EXCHANGE,
            routing_key=routing_key,
            body=json.dumps(payload),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )


# ---------------------------------------------------------------------------
# Worker
# ---------------------------------------------------------------------------


class Worker:
    def __init__(
        self,
        rabbitmq_url: str = RABBITMQ_URL,
        provider_url: str = PROVIDER_URL,
    ) -> None:
        self._rabbitmq_url = rabbitmq_url
        self._provider_url = provider_url

    def run(self) -> None:
        provider = ProviderClient(self._provider_url)

        log.info("Connecting to RabbitMQ → %s", self._rabbitmq_url)
        connection = pika.BlockingConnection(pika.URLParameters(self._rabbitmq_url))
        channel    = connection.channel()
        channel.basic_qos(prefetch_count=1)

        handler = JobHandler(channel, provider)

        def _safe_nack(ch: pika.channel.Channel, delivery_tag: int) -> None:
            """Best-effort nack. If the connection itself is already gone
            (e.g. a dropped TCP stream), there's nothing more to do here —
            RabbitMQ will redeliver once it notices the consumer vanished,
            and start_consuming()'s caller handles the resulting disconnect.
            Letting that secondary failure escape uncaught is what used to
            crash the whole worker with an unrelated ChannelWrongStateError
            on top of the original connection loss."""
            try:
                ch.basic_nack(delivery_tag=delivery_tag, requeue=True)
            except pika.exceptions.AMQPError as exc:
                log.error("Could not nack message — connection already lost: %s", exc)

        def _callback(
            ch: pika.channel.Channel,
            method: pika.spec.Basic.Deliver,
            _props: pika.spec.BasicProperties,
            body: bytes,
        ) -> None:
            try:
                handler.handle(method, body)
            except ProviderError as exc:
                log.error("Provider unreachable — nacking for requeue: %s", exc)
                _safe_nack(ch, method.delivery_tag)
            except Exception as exc:
                log.exception("Unhandled crash processing message — nacking for requeue: %s", exc)
                _safe_nack(ch, method.delivery_tag)

        channel.basic_consume(queue=SCRAPE_JOBS_QUEUE, on_message_callback=_callback)
        log.info("Listening on '%s'. Ctrl-C to stop.", SCRAPE_JOBS_QUEUE)

        try:
            channel.start_consuming()
        except KeyboardInterrupt:
            log.info("Shutting down…")
            channel.stop_consuming()
        except pika.exceptions.AMQPError as exc:
            log.error("Lost connection to RabbitMQ — exiting: %s", exc)
        finally:
            try:
                connection.close()
            except pika.exceptions.AMQPError as exc:
                log.debug("Connection already closed: %s", exc)


if __name__ == "__main__":
    Worker().run()
