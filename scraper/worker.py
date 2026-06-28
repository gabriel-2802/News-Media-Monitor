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

Article message format (JSON, published to news_monitor exchange → articles queue):
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
    Individual article failures (bad page) are logged but do not retry the job.

Run:
    python worker.py

Environment variables:
    RABBITMQ_URL     amqp://user:pass@host:port/vhost   (default: local dev)
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
from typing import Optional

import pika
import pika.channel
import pika.spec
import requests
from neo4j import GraphDatabase, Driver
from neo4j.exceptions import Neo4jError

from news_scraper import (
    ExtractionError,
    FetchError,
    extract_body,
    fetch_html,
    parse_rss,
)

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

RABBITMQ_URL   = os.getenv("RABBITMQ_URL",    "amqp://admin:secret@localhost:5672/news_monitor")
NEO4J_URI      = os.getenv("NEO4J_URI",       "bolt://localhost:7687")
NEO4J_USER     = os.getenv("NEO4J_USER",      "neo4j")
NEO4J_PASSWORD = os.getenv("NEO4J_PASSWORD",  "secretsecret")

SCRAPE_JOBS_QUEUE        = "scrape.jobs"
NEWS_EXCHANGE            = "news_monitor"
SCRAPE_JOB_ROUTING_KEY  = "scrape.job"
ARTICLE_ROUTING_KEY     = "article.scraped"
SOURCE_FAILED_ROUTING_KEY = "source.failed"
MAX_RETRIES = 3

USER_AGENT = "NewsMonitorBot/0.1 (research scraper; contact: gabrielvalentine738@gmail.com)"
BROWSER_UA = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)
RATE_LIMIT_SECONDS = 2.0

# Sources that block bot UAs on their RSS endpoint
RSS_BROWSER_UA_SOURCES: set[str] = {"cbc"}

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Neo4j
# ---------------------------------------------------------------------------

def article_exists(driver: Driver, url: str) -> bool:
    with driver.session() as s:
        return s.run("MATCH (a:Article {url: $url}) RETURN 1 LIMIT 1", url=url).single() is not None


def _to_localdatetime_str(dt: Optional[datetime]) -> Optional[str]:
    if dt is None:
        return None
    return dt.astimezone(timezone.utc).replace(tzinfo=None).isoformat()


def save_article(
    driver: Driver,
    source_name: str,
    url: str,
    title: str,
    author: Optional[str],
    published_at: Optional[datetime],
    body_text: str,
) -> None:
    pub_str = _to_localdatetime_str(published_at)
    with driver.session() as s:
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
            MATCH (src:NewsSource {name: $source_name})
            MERGE (src)-[:PUBLISHED]->(a)
            """,
            source_name=source_name,
            url=url,
            title=title,
            author=author,
            pub_str=pub_str,
            body_text=body_text,
        )


# ---------------------------------------------------------------------------
# RabbitMQ
# ---------------------------------------------------------------------------

def _publish(channel: pika.channel.Channel, routing_key: str, payload: dict) -> None:
    channel.basic_publish(
        exchange=NEWS_EXCHANGE,
        routing_key=routing_key,
        body=json.dumps(payload),
        properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
    )


def _handle_failure(
    channel: pika.channel.Channel,
    job: dict,
    retry_count: int,
    reason: str,
) -> None:
    source_name = job["name"]
    next_attempt = retry_count + 1
    if next_attempt >= MAX_RETRIES:
        log.error("[%s] Exhausted %d retries (%s) — publishing source.failed", source_name, MAX_RETRIES, reason)
        _publish(channel, SOURCE_FAILED_ROUTING_KEY, {
            "source_name": source_name,
            "reason": reason,
            "failed_at": datetime.now(timezone.utc).isoformat(),
            "retries": next_attempt,
        })
    else:
        log.warning("[%s] Attempt %d/%d failed (%s) — re-queuing", source_name, next_attempt, MAX_RETRIES, reason)
        _publish(channel, SCRAPE_JOB_ROUTING_KEY, {**job, "retry_count": next_attempt})


# ---------------------------------------------------------------------------
# Job handler
# ---------------------------------------------------------------------------

def handle_job(
    channel: pika.channel.Channel,
    method: pika.spec.Basic.Deliver,
    body: bytes,
    driver: Driver,
) -> None:
    try:
        payload = json.loads(body)
        source_name: str = payload["name"]
        rss_url: str     = payload["rss_url"]
        retry_count: int = int(payload.get("retry_count", 0))
    except (json.JSONDecodeError, KeyError, ValueError) as exc:
        log.error("Malformed message, discarding: %r — %s", body, exc)
        channel.basic_ack(delivery_tag=method.delivery_tag)
        return

    log.info("[%s] Scrape job started (attempt %d/%d)", source_name, retry_count + 1, MAX_RETRIES)

    http = requests.Session()
    http.headers["User-Agent"] = USER_AGENT

    # Fetch RSS
    try:
        if source_name in RSS_BROWSER_UA_SOURCES:
            http.headers["User-Agent"] = BROWSER_UA
        feed_html = fetch_html(rss_url, http)
    except FetchError as exc:
        _handle_failure(channel, payload, retry_count, f"RSS fetch: {exc}")
        channel.basic_ack(delivery_tag=method.delivery_tag)
        return
    finally:
        http.headers["User-Agent"] = USER_AGENT

    time.sleep(RATE_LIMIT_SECONDS)

    # Parse RSS
    try:
        entries = parse_rss(feed_html)
    except Exception as exc:
        _handle_failure(channel, payload, retry_count, f"RSS parse: {exc}")
        channel.basic_ack(delivery_tag=method.delivery_tag)
        return

    if not entries:
        log.warning("[%s] RSS returned no entries", source_name)
        channel.basic_ack(delivery_tag=method.delivery_tag)
        return

    saved = skipped = failed = 0

    for entry in entries:
        # Dedup — skip URLs already in Neo4j
        try:
            if article_exists(driver, entry.url):
                skipped += 1
                continue
        except Neo4jError as exc:
            log.warning("[%s] Dedup check failed (%s): %s", source_name, entry.url, exc)
            failed += 1
            continue

        # Fetch article body
        try:
            html = fetch_html(entry.url, http)
            body_text = extract_body(html, entry.url)
        except (FetchError, ExtractionError) as exc:
            log.warning("[%s] Article skipped (%s): %s", source_name, entry.url, exc)
            failed += 1
            time.sleep(RATE_LIMIT_SECONDS)
            continue

        # Save to Neo4j
        try:
            save_article(
                driver=driver,
                source_name=source_name,
                url=entry.url,
                title=entry.title,
                author=entry.author,
                published_at=entry.published_at,
                body_text=body_text,
            )
        except Neo4jError as exc:
            log.warning("[%s] Neo4j save failed (%s): %s", source_name, entry.url, exc)
            failed += 1
            time.sleep(RATE_LIMIT_SECONDS)
            continue

        # Publish to articles queue
        pub_str = _to_localdatetime_str(entry.published_at)
        _publish(channel, ARTICLE_ROUTING_KEY, {
            "source_name": source_name,
            "url": entry.url,
            "title": entry.title,
            "author": entry.author,
            "published_at": pub_str,
            "body_text": body_text,
        })

        saved += 1
        log.info("[%s] Saved + published: %s", source_name, entry.title[:80])
        time.sleep(RATE_LIMIT_SECONDS)

    log.info("[%s] Done — saved: %d  skipped: %d  failed: %d", source_name, saved, skipped, failed)
    channel.basic_ack(delivery_tag=method.delivery_tag)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def run() -> None:
    log.info("Connecting to Neo4j  → %s", NEO4J_URI)
    driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))
    driver.verify_connectivity()
    log.info("Neo4j connected.")

    log.info("Connecting to RabbitMQ → %s", RABBITMQ_URL)
    connection = pika.BlockingConnection(pika.URLParameters(RABBITMQ_URL))
    channel = connection.channel()
    channel.basic_qos(prefetch_count=1)

    def _callback(
        ch: pika.channel.Channel,
        method: pika.spec.Basic.Deliver,
        _properties: pika.spec.BasicProperties,
        body: bytes,
    ) -> None:
        handle_job(ch, method, body, driver)

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
    run()
