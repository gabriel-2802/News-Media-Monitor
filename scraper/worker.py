"""
RabbitMQ-driven scrape worker.

Consumes scrape job messages from the `scrape.jobs` queue, fetches the RSS
feed, saves new articles to Neo4j, and publishes each saved article to the
`articles` queue (skipping URLs already in Neo4j).

Scrape job message format (JSON):
    {
        "name": "bbc",
        "rss_url": "https://feeds.bbci.co.uk/news/world/rss.xml",
        "base_url": "https://www.bbc.co.uk",
        "retry_count": 0          # optional, defaults to 0
    }

Article message format (JSON):
    {
        "source_name": "bbc",
        "url": "...",
        "title": "...",
        "author": "...",          # null if not found
        "published_at": "...",    # ISO 8601 UTC, null if not found
        "body_text": "..."
    }

Retry behaviour:
    RSS fetch or parse failure → re-publish job with retry_count+1 and ack original.
    After MAX_RETRIES failures → publish source.failed event and drop the job.
    Individual article failures are logged but do not retry the job.

Run:
    python worker.py

Environment variables:
    RABBITMQ_URL     amqp://user:pass@host:port/vhost
    NEO4J_URI        bolt://host:port
    NEO4J_USER       neo4j
    NEO4J_PASSWORD   secretsecret
"""
from __future__ import annotations

import json
import logging
import os
import time
from datetime import datetime, timezone
from typing import Any, Optional

import pika
import pika.channel
import pika.spec
from neo4j import Driver, GraphDatabase
from neo4j.exceptions import Neo4jError

from news_scraper import (
    Article,
    BaseStorage,
    ExtractionError,
    FetchError,
    HttpFetcher,
    PlaywrightFetcher,
    SmartFetcher,
    RSSEntry,
    SourceConfig,
    TrafilaturaExtractor,
    BROWSER_UA,
    RATE_LIMIT_SECONDS,
    USER_AGENT,
    parse_rss,
)

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

RABBITMQ_URL      = os.getenv("RABBITMQ_URL",    "amqp://admin:secret@localhost:5672/news_monitor")
NEO4J_URI         = os.getenv("NEO4J_URI",       "bolt://localhost:7687")
NEO4J_USER        = os.getenv("NEO4J_USER",      "neo4j")
NEO4J_PASSWORD    = os.getenv("NEO4J_PASSWORD",  "secretsecret")

SCRAPE_JOBS_QUEUE         = "scrape.jobs"
NEWS_EXCHANGE             = "news_monitor"
SCRAPE_JOB_ROUTING_KEY   = "scrape.job"
ARTICLE_ROUTING_KEY       = "article.scraped"
SOURCE_FAILED_ROUTING_KEY = "source.failed"
MAX_RETRIES               = 3

RSS_BROWSER_UA_SOURCES: frozenset[str] = frozenset({"cbc"})

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Neo4j storage (implements BaseStorage from news_scraper)
# ---------------------------------------------------------------------------


class Neo4jStorage(BaseStorage):
    def __init__(self, driver: Driver) -> None:
        self._driver = driver

    def exists(self, url: str) -> bool:
        with self._driver.session() as s:
            return s.run(
                "MATCH (a:Article {url: $url}) RETURN 1 LIMIT 1", url=url
            ).single() is not None

    def is_source_disabled(self, source_name: str) -> bool:
        with self._driver.session() as s:
            row = s.run(
                "MATCH (src:NewsSource {name: $name}) RETURN src.is_disabled AS disabled",
                name=source_name,
            ).single()
            return bool(row and row["disabled"])

    def record_source_failure(self, source_name: str, reason: str, max_failures: int) -> bool:
        """Increment failure_count on the NewsSource. Returns True if the source is now disabled."""
        with self._driver.session() as s:
            row = s.run(
                """
                MERGE (src:NewsSource {name: $name})
                WITH src, coalesce(src.failure_count, 0) + 1 AS new_count
                SET src.failure_count       = new_count,
                    src.last_failure_reason = $reason,
                    src.last_failed_at      = localdatetime(),
                    src.is_disabled         = new_count >= $max_failures
                RETURN src.failure_count AS count, src.is_disabled AS disabled
                """,
                name=source_name,
                reason=reason,
                max_failures=max_failures,
            ).single()
            return bool(row and row["disabled"])

    def reset_source_failures(self, source_name: str) -> None:
        with self._driver.session() as s:
            s.run(
                """
                MERGE (src:NewsSource {name: $name})
                SET src.failure_count = 0, src.is_disabled = false
                """,
                name=source_name,
            )

    def save(self, article: Article) -> None:
        pub_str = self._to_neo4j_dt(article.published_at)
        with self._driver.session() as s:
            s.run(
                """
                MERGE (a:Article {url: $url})
                ON CREATE SET
                  a.title        = $title,
                  a.author       = $author,
                  a.source_name  = $source_name,
                  a.published_at = CASE WHEN $pub_str IS NOT NULL
                                        THEN localdatetime($pub_str)
                                        ELSE null END,
                  a.body_text    = $body_text,
                  a.description  = null
                WITH a
                MERGE (src:NewsSource {name: $source_name})
                MERGE (src)-[:PUBLISHED]->(a)
                """,
                source_name=article.source,
                url=article.url,
                title=article.title,
                author=article.author,
                pub_str=pub_str,
                body_text=article.body_text,
            )

    @staticmethod
    def _to_neo4j_dt(dt: Optional[datetime]) -> Optional[str]:
        if dt is None:
            return None
        return dt.astimezone(timezone.utc).replace(tzinfo=None).isoformat()


# ---------------------------------------------------------------------------
# Job handler
# ---------------------------------------------------------------------------


class JobHandler:
    def __init__(
        self,
        channel: pika.channel.Channel,
        driver: Driver,
        max_retries: int = MAX_RETRIES,
        rate_limit: float = RATE_LIMIT_SECONDS,
    ) -> None:
        self._channel         = channel
        self._storage         = Neo4jStorage(driver)
        self._extractor       = TrafilaturaExtractor()
        self._max_retries     = max_retries
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
            rss_url: str     = payload["rss_url"]
            retry_count: int = int(payload.get("retry_count", 0))
        except (json.JSONDecodeError, KeyError, ValueError) as exc:
            log.error("Malformed message, discarding: %r — %s", body, exc)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        log.info("[%s] Scrape job (attempt %d/%d)", source_name, retry_count + 1, self._max_retries)

        if self._storage.is_source_disabled(source_name):
            log.warning("[%s] Source is disabled (too many failures) — skipping", source_name)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        feed_fetcher = (
            self._feed_http_bot
            if source_name in RSS_BROWSER_UA_SOURCES
            else self._feed_http
        )

        try:
            feed_html = feed_fetcher.fetch(rss_url)
            entries   = parse_rss(feed_html)
        except FetchError as exc:
            reason = f"RSS fetch: {exc}"
            disabled = self._storage.record_source_failure(source_name, reason, self._max_retries)
            if disabled:
                log.error("[%s] Disabled after %d failures: %s", source_name, self._max_retries, reason)
            else:
                self._fail(payload, retry_count, reason)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return
        except Exception as exc:
            reason = f"RSS parse: {exc}"
            disabled = self._storage.record_source_failure(source_name, reason, self._max_retries)
            if disabled:
                log.error("[%s] Disabled after %d failures: %s", source_name, self._max_retries, reason)
            else:
                self._fail(payload, retry_count, reason)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        if not entries:
            log.warning("[%s] RSS returned no entries", source_name)
            self._channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        time.sleep(self._rate_limit)
        saved = skipped = failed = 0

        for entry in entries:
            try:
                if self._storage.exists(entry.url):
                    skipped += 1
                    continue
            except Neo4jError as exc:
                log.warning("[%s] Dedup check failed (%s): %s", source_name, entry.url, exc)
                failed += 1
                continue

            try:
                html      = self._article_fetcher.fetch(entry.url)
                body_text = self._extractor.extract(html, entry.url)
            except (FetchError, ExtractionError) as exc:
                log.warning("[%s] Article skipped (%s): %s", source_name, entry.url, exc)
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
                source=source_name,
            )

            try:
                self._storage.save(article)
            except Neo4jError as exc:
                log.warning("[%s] Neo4j save failed (%s): %s", source_name, entry.url, exc)
                failed += 1
                time.sleep(self._rate_limit)
                continue

            self._publish(ARTICLE_ROUTING_KEY, self._article_payload(article))
            saved += 1
            log.info("[%s] Saved + published: %s", source_name, article.title[:80])
            time.sleep(self._rate_limit)

        log.info("[%s] Done — saved=%d skipped=%d failed=%d", source_name, saved, skipped, failed)
        # Successful RSS fetch — clear any previous failure streak
        self._storage.reset_source_failures(source_name)
        self._channel.basic_ack(delivery_tag=method.delivery_tag)

    # ------------------------------------------------------------------

    def _fail(self, job: dict[str, Any], retry_count: int, reason: str) -> None:
        name = job["name"]
        next_attempt = retry_count + 1
        if next_attempt >= self._max_retries:
            log.error("[%s] Exhausted retries (%s) — source.failed", name, reason)
            self._publish(SOURCE_FAILED_ROUTING_KEY, {
                "source_name": name,
                "reason":      reason,
                "failed_at":   datetime.now(timezone.utc).isoformat(),
                "retries":     next_attempt,
            })
        else:
            log.warning("[%s] Attempt %d/%d failed (%s) — re-queuing", name, next_attempt, self._max_retries, reason)
            self._publish(SCRAPE_JOB_ROUTING_KEY, {**job, "retry_count": next_attempt})

    def _publish(self, routing_key: str, payload: dict[str, Any]) -> None:
        self._channel.basic_publish(
            exchange=NEWS_EXCHANGE,
            routing_key=routing_key,
            body=json.dumps(payload),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )

    @staticmethod
    def _article_payload(article: Article) -> dict[str, Any]:
        pub_str: Optional[str] = (
            article.published_at.astimezone(timezone.utc).replace(tzinfo=None).isoformat()
            if article.published_at else None
        )
        return {
            "source_name":  article.source,
            "url":          article.url,
            "title":        article.title,
            "author":       article.author,
            "published_at": pub_str,
            "body_text":    article.body_text,
        }


# ---------------------------------------------------------------------------
# Worker
# ---------------------------------------------------------------------------


class Worker:
    def __init__(
        self,
        rabbitmq_url: str   = RABBITMQ_URL,
        neo4j_uri: str      = NEO4J_URI,
        neo4j_user: str     = NEO4J_USER,
        neo4j_password: str = NEO4J_PASSWORD,
    ) -> None:
        self._rabbitmq_url   = rabbitmq_url
        self._neo4j_uri      = neo4j_uri
        self._neo4j_user     = neo4j_user
        self._neo4j_password = neo4j_password

    def run(self) -> None:
        log.info("Connecting to Neo4j → %s", self._neo4j_uri)
        driver = GraphDatabase.driver(self._neo4j_uri, auth=(self._neo4j_user, self._neo4j_password))
        driver.verify_connectivity()
        log.info("Neo4j connected.")

        log.info("Connecting to RabbitMQ → %s", self._rabbitmq_url)
        connection = pika.BlockingConnection(pika.URLParameters(self._rabbitmq_url))
        channel    = connection.channel()
        channel.basic_qos(prefetch_count=1)

        handler = JobHandler(channel, driver)

        def _callback(
            ch: pika.channel.Channel,
            method: pika.spec.Basic.Deliver,
            _props: pika.spec.BasicProperties,
            body: bytes,
        ) -> None:
            try:
                handler.handle(method, body)
            except Exception as exc:
                log.exception("Unhandled crash processing message — nacking for requeue: %s", exc)
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

        channel.basic_consume(queue=SCRAPE_JOBS_QUEUE, on_message_callback=_callback)
        log.info("Listening on '%s'. Ctrl-C to stop.", SCRAPE_JOBS_QUEUE)

        try:
            channel.start_consuming()
        except KeyboardInterrupt:
            log.info("Shutting down…")
            channel.stop_consuming()
        finally:
            connection.close()
            driver.close()


if __name__ == "__main__":
    Worker().run()
