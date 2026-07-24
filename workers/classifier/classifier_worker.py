"""
RabbitMQ-driven classification worker.

Neo4j is never touched directly — every read/write of article data goes
through the Provider service's REST API, same as the scrape worker.

Consumes messages from the `article.classify` queue (published by the scrape
worker after each article is saved — see scraper_worker.py). For each
message:

  - Fetches the article's body text from the Provider.
  - Classifies it into a topic using the fine-tuned NewsClassifier model.
  - Writes the topic back to the article via the Provider.

Message format (JSON), published by worker.py:
    {
        "url": "...",
        "source_name": "bbc"
    }

This worker only classifies topic; it does not touch Story/embedding logic.
Clustering runs as an independent consumer of the same scrape-worker
fan-out, off its own `article.cluster` queue (see clusterer_worker.py and
rabbitmq/setup.sh).

Run (from workers/, the parent of this file):
    make classify-worker
    # or: classifier/venv/bin/python -m classifier.classifier_worker

Environment variables:
    RABBITMQ_URL     amqp://user:pass@host:port/vhost
    PROVIDER_URL     http://host:port
"""
from __future__ import annotations

import json
import logging

import pika
import pika.channel
import pika.exceptions
import pika.spec

from classifier.classifier import NewsClassifier
from env_config import require_env
from log_config import configure_logging
from provider_client import ProviderClient, ProviderError
from retry import call_with_retry

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

RABBITMQ_URL = require_env("RABBITMQ_URL")
PROVIDER_URL = require_env("PROVIDER_URL")

ARTICLE_CLASSIFY_QUEUE = require_env("ARTICLE_CLASSIFY_QUEUE")

configure_logging()
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Job handler
# ---------------------------------------------------------------------------


class ClassificationHandler:
    def __init__(self, provider: ProviderClient) -> None:
        self._provider = provider
        # Triggers model load on first use, not at import time — keeps
        # worker startup fast if this ever gets imported without immediately
        # running (e.g. tests).
        self._classifier = NewsClassifier.get_instance()

    def handle(self, method: pika.spec.Basic.Deliver, body: bytes, channel: pika.channel.Channel) -> None:
        try:
            payload = json.loads(body)
            url: str = payload["url"]
        except (json.JSONDecodeError, KeyError) as exc:
            log.error("Malformed message, discarding: %r — %s", body, exc)
            channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        article = self._provider.get_article(url)
        if article is None:
            log.warning("Article not found via provider, discarding: %s", url)
            channel.basic_ack(delivery_tag=method.delivery_tag)
            return

        # Title is a strong topic signal and cheap to include; truncation to
        # the model's max_length happens inside classify() either way.
        text = f"{article.title}\n\n{article.body_text}"
        topic = self._classifier.classify(text)

        if self._provider.set_article_topic(url, topic):
            log.info("Classified: %s -> %s", url, topic)
        else:
            log.warning("Provider rejected topic update: %s -> %s", url, topic)

        channel.basic_ack(delivery_tag=method.delivery_tag)


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

        log.info("Loading classifier model…")
        handler = ClassificationHandler(provider)
        log.info("Model loaded.")

        log.info("Connecting to RabbitMQ → %s", self._rabbitmq_url)
        connection = pika.BlockingConnection(pika.URLParameters(self._rabbitmq_url))
        channel = connection.channel()
        # prefetch_count=1: classification is CPU-bound (or GPU-bound); don't
        # let RabbitMQ hand this worker a backlog it can't process concurrently.
        channel.basic_qos(prefetch_count=1)

        def _safe_nack(ch: pika.channel.Channel, delivery_tag: int) -> None:
            """Best-effort nack. If the connection itself is already gone,
            there's nothing more to do here — RabbitMQ will redeliver once
            it notices the consumer vanished. Letting that secondary
            failure escape uncaught is what used to crash the whole worker
            with an unrelated ChannelWrongStateError on top of the original
            connection loss."""
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
                call_with_retry(lambda: handler.handle(method, body, ch), connection.sleep)
            except ProviderError as exc:
                log.error(
                    "Provider still unreachable — nacking for requeue and shutting down: %s", exc
                )
                _safe_nack(ch, method.delivery_tag)
                ch.stop_consuming()
            except Exception as exc:
                log.exception("Unhandled crash processing message — nacking for requeue: %s", exc)
                _safe_nack(ch, method.delivery_tag)

        channel.basic_consume(queue=ARTICLE_CLASSIFY_QUEUE, on_message_callback=_callback)
        log.info("Listening on '%s'. Ctrl-C to stop.", ARTICLE_CLASSIFY_QUEUE)

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