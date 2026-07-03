"""
RabbitMQ-driven classification worker.

Neo4j is never touched directly — every read/write of article data goes
through the Provider service's REST API, same as the scrape worker.

Consumes messages from the `clustering` queue (published by the scrape
worker after each article is saved). For each message:

  - Fetches the article's body text from the Provider.
  - Classifies it into a topic using the fine-tuned NewsClassifier model.
  - Writes the topic back to the article via the Provider.

Message format (JSON), published by worker.py:
    {
        "url": "...",
        "source_name": "bbc"
    }

This queue is also where the *next* stage (embedding + clustering) will
eventually consume from. For now this worker only classifies topic; it does
not touch Story/embedding logic — see the architecture plan for how that
gets layered in later as a second consumer of the same queue, or a
follow-on queue if classification should gate clustering.

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
import os

import pika
import pika.channel
import pika.spec

from classifier.classifier import NewsClassifier
from provider_client import ProviderClient, ProviderError

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://admin:secret@localhost:5672/news_monitor")
PROVIDER_URL = os.getenv("PROVIDER_URL", "http://localhost:8080")

CLUSTERING_QUEUE = "clustering"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(message)s",
    datefmt="%H:%M:%S",
)
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

        def _callback(
            ch: pika.channel.Channel,
            method: pika.spec.Basic.Deliver,
            _props: pika.spec.BasicProperties,
            body: bytes,
        ) -> None:
            try:
                handler.handle(method, body, ch)
            except ProviderError as exc:
                log.error("Provider unreachable — nacking for requeue: %s", exc)
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
            except Exception as exc:
                log.exception("Unhandled crash processing message — nacking for requeue: %s", exc)
                ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

        channel.basic_consume(queue=CLUSTERING_QUEUE, on_message_callback=_callback)
        log.info("Listening on '%s'. Ctrl-C to stop.", CLUSTERING_QUEUE)

        try:
            channel.start_consuming()
        except KeyboardInterrupt:
            log.info("Shutting down…")
            channel.stop_consuming()
        finally:
            connection.close()


if __name__ == "__main__":
    Worker().run()