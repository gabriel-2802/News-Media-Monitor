"""
RabbitMQ-driven clustering worker.

Neo4j is never touched directly — every read/write of Story/Article data
goes through the Provider service's REST API, same as the scrape and
classifier workers. Embeddings live in Qdrant (`story_centroids`
collection), keyed by Story ID, never in Neo4j.

Consumes messages from the `embedding` queue. This queue is bound to the
same `article.clustering` routing key the scrape worker already publishes
to after every article save (see scraper_worker.py) — it runs as an
independent second consumer alongside the classifier worker rather than
competing with it for the same messages (see rabbitmq/setup.sh).

For each message:
  - Fetches the article's body text from the Provider.
  - Embeds it (see embedder.py).
  - Queries `story_centroids` for the nearest centroid updated within
    RECENCY_WINDOW_DAYS.
  - If the best match's score >= SIMILARITY_THRESHOLD, attaches the
    article to that Story (Provider) and updates its centroid as a
    running average (Qdrant).
  - Otherwise creates a new Story (Provider — this is the one place the
    Provider write has to happen *before* the Qdrant write, since Story
    IDs are server-generated and there's no way to seed a centroid point
    under an ID that doesn't exist yet) and seeds its centroid.
  - No candidates (bootstrap / first article ever) falls into the same
    below-threshold branch — no special-casing needed.

Message format (JSON), published by worker.py:
    {
        "url": "...",
        "source_name": "bbc"
    }

Run (from workers/, the parent of this file):
    make cluster-worker
    # or: clusterer/venv/bin/python -m clusterer.clusterer_worker

Environment variables:
    RABBITMQ_URL      amqp://user:pass@host:port/vhost
    PROVIDER_URL      http://host:port
    QDRANT_URL        https://<cluster-id>.<region>.gcp.cloud.qdrant.io
    QDRANT_API_KEY    Qdrant Cloud API key
"""
from __future__ import annotations

import json
import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Optional

import pika
import pika.channel
import pika.spec
from qdrant_client import QdrantClient
from qdrant_client.models import (
    DatetimeRange,
    Distance,
    FieldCondition,
    Filter,
    PayloadSchemaType,
    PointStruct,
    VectorParams,
)

from clusterer.embedder import ArticleEmbedder
from env_config import require_env, require_float, require_int
from provider_client import ProviderClient, ProviderError

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

RABBITMQ_URL = require_env("RABBITMQ_URL")
PROVIDER_URL = require_env("PROVIDER_URL")

EMBEDDING_QUEUE = require_env("EMBEDDING_QUEUE")
CENTROID_COLLECTION = require_env("CENTROID_COLLECTION")

# Must match ArticleEmbedder's output dim (all-MiniLM-L6-v2 = 384). Update
# alongside embedder.py's MODEL_NAME if that ever changes — a mismatch
# fails loudly at collection-creation time, not silently.
EMBEDDING_DIM = require_int("EMBEDDING_DIM")

# Chosen from workers/clusterer/data/output/validation_report.md (the
# all-MiniLM-L6-v2 run, since that's what embedder.py uses). Revisit once
# real traffic accumulates.
SIMILARITY_THRESHOLD = require_float("SIMILARITY_THRESHOLD")
RECENCY_WINDOW_DAYS = require_int("RECENCY_WINDOW_DAYS")

# The model has no automatic truncation for inputs this short of its
# max_seq_length, so an unusually long scraped article can drive quadratic
# self-attention memory into the tens-of-GB range. Cap before embedding —
# see validate.py, where this was found live.
_MAX_EMBED_CHARS = require_int("MAX_EMBED_CHARS")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


def _embed_article(embedder: ArticleEmbedder, title: str, body_text: str) -> list[float]:
    text = f"{title}\n\n{body_text}"[:_MAX_EMBED_CHARS]
    return embedder.embed(text)


def _normalize(vector: list[float]) -> list[float]:
    norm = sum(v * v for v in vector) ** 0.5
    if norm == 0:
        return vector
    return [v / norm for v in vector]


def ensure_collection(qdrant: QdrantClient) -> None:
    """Idempotent — safe to call on every worker startup."""
    if not qdrant.collection_exists(CENTROID_COLLECTION):
        log.info("Creating Qdrant collection '%s' (dim=%d, cosine)", CENTROID_COLLECTION, EMBEDDING_DIM)
        qdrant.create_collection(
            collection_name=CENTROID_COLLECTION,
            vectors_config=VectorParams(size=EMBEDDING_DIM, distance=Distance.COSINE),
        )

    # Qdrant requires a payload index before a field can be used in a query
    # filter — without this, the recency-window filter in
    # _find_best_candidate fails with HTTP 400. Kept outside the "just
    # created" branch above (and re-run on every startup) so an existing
    # collection from before this index existed still gets it.
    qdrant.create_payload_index(
        collection_name=CENTROID_COLLECTION,
        field_name="lastUpdated",
        field_schema=PayloadSchemaType.DATETIME,
    )


# ---------------------------------------------------------------------------
# Job handler
# ---------------------------------------------------------------------------


class ClusteringHandler:
    def __init__(self, provider: ProviderClient, qdrant: QdrantClient) -> None:
        self._provider = provider
        self._qdrant = qdrant
        # Triggers model load on first use, not at import time — keeps
        # worker startup fast if this ever gets imported without immediately
        # running (e.g. tests).
        self._embedder = ArticleEmbedder.get_instance()

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

        vector = _embed_article(self._embedder, article.title, article.body_text)
        best_hit = self._find_best_candidate(vector)

        if best_hit is not None and best_hit.score >= SIMILARITY_THRESHOLD:
            self._attach_to_existing(best_hit, vector, url)
        else:
            self._create_and_attach(article.title, vector, url)

        channel.basic_ack(delivery_tag=method.delivery_tag)

    def _find_best_candidate(self, vector: list[float]) -> Optional[Any]:
        cutoff = (datetime.now(timezone.utc) - timedelta(days=RECENCY_WINDOW_DAYS)).isoformat()
        hits = self._qdrant.query_points(
            collection_name=CENTROID_COLLECTION,
            query=vector,
            query_filter=Filter(must=[FieldCondition(key="lastUpdated", range=DatetimeRange(gte=cutoff))]),
            limit=1,
            with_vectors=True,
        ).points
        return hits[0] if hits else None

    def _attach_to_existing(self, hit: Any, vector: list[float], url: str) -> None:
        story_id = hit.payload["storyId"]

        # Vector DB first per the consistency strategy (idempotent upsert
        # by ID, cheap to retry). NOTE: unlike a plain upsert-by-ID, this
        # centroid update depends on the *previous* count/vector, so a
        # transient-failure retry that re-runs this whole handler will
        # compound the running average an extra time if the Provider write
        # below succeeded on a prior attempt but the ack was lost. Rare in
        # practice (requires a crash between the two writes), and only
        # skews the internal centroid slightly — Neo4j's articleCount
        # stays authoritative. Acceptable for v1.
        old_count = hit.payload["articleCount"]
        new_count = old_count + 1
        new_vector = _normalize([(ov * old_count + nv) for ov, nv in zip(hit.vector, vector)])

        self._qdrant.upsert(
            collection_name=CENTROID_COLLECTION,
            points=[PointStruct(
                id=story_id,
                vector=new_vector,
                payload={
                    "storyId": story_id,
                    "articleCount": new_count,
                    "lastUpdated": datetime.now(timezone.utc).isoformat(),
                },
            )],
        )

        attached = self._provider.attach_article_to_story(story_id, url)
        if attached is not None:
            log.info("Attached (score=%.3f): %s -> story %s", hit.score, url, story_id)
        else:
            log.warning("Provider rejected attach to existing story %s: %s", story_id, url)

    def _create_and_attach(self, title: str, vector: list[float], url: str) -> None:
        # Provider first here — Story IDs are server-generated
        # (@GeneratedValue), so there's no ID to seed a centroid point
        # under until the Provider assigns one.
        new_story = self._provider.create_story(title)
        attached = self._provider.attach_article_to_story(new_story.id, url)
        if attached is None:
            log.warning("Provider rejected attach to freshly created story %s: %s", new_story.id, url)
            return

        self._qdrant.upsert(
            collection_name=CENTROID_COLLECTION,
            points=[PointStruct(
                id=new_story.id,
                vector=vector,
                payload={
                    "storyId": new_story.id,
                    "articleCount": 1,
                    "lastUpdated": datetime.now(timezone.utc).isoformat(),
                },
            )],
        )
        log.info("New story: %s -> story %s", url, new_story.id)


# ---------------------------------------------------------------------------
# Worker
# ---------------------------------------------------------------------------


class Worker:
    def __init__(
        self,
        rabbitmq_url: str = RABBITMQ_URL,
        provider_url: str = PROVIDER_URL,
        qdrant_url: Optional[str] = None,
        qdrant_api_key: Optional[str] = None,
    ) -> None:
        self._rabbitmq_url = rabbitmq_url
        self._provider_url = provider_url
        self._qdrant_url = qdrant_url or require_env("QDRANT_URL")
        self._qdrant_api_key = qdrant_api_key or require_env("QDRANT_API_KEY")

    def run(self) -> None:
        provider = ProviderClient(self._provider_url)
        qdrant = QdrantClient(url=self._qdrant_url, api_key=self._qdrant_api_key)
        ensure_collection(qdrant)

        log.info("Loading embedding model…")
        handler = ClusteringHandler(provider, qdrant)
        log.info("Model loaded.")

        log.info("Connecting to RabbitMQ → %s", self._rabbitmq_url)
        connection = pika.BlockingConnection(pika.URLParameters(self._rabbitmq_url))
        channel = connection.channel()
        # prefetch_count=1: embedding + candidate scoring is CPU/GPU-bound;
        # don't let RabbitMQ hand this worker a backlog it can't process
        # concurrently.
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

        channel.basic_consume(queue=EMBEDDING_QUEUE, on_message_callback=_callback)
        log.info("Listening on '%s'. Ctrl-C to stop.", EMBEDDING_QUEUE)

        try:
            channel.start_consuming()
        except KeyboardInterrupt:
            log.info("Shutting down…")
            channel.stop_consuming()
        finally:
            connection.close()


if __name__ == "__main__":
    Worker().run()
