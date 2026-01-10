from sentence_transformers import SentenceTransformer
from typing import List, Tuple, Dict
from collections.abc import Generator
import psycopg2
import numpy as np
import faiss
import os

from embedding import TextEmbedder

# article fetching MACROs
ArticleRow = Tuple[int, str, str, str]  # (id, title, description, content)

FETCH_QUERY = "SELECT article_id, title, summary, content FROM articles WHERE cluster_id IS NULL LIMIT %s OFFSET %s"

FETCH_QUERY_TIME_LIMIT = "SELECT article_id, title, summary, content FROM articles WHERE (cluster_id IS NULL) AND  (published >= CURRENT_DATE LIMIT %s OFFSET %s)"

INSERT_CLUSTER_QUERY = "INSERT INTO article_clusters DEFAULT VALUES RETURNING cluster_id"

UPDATE_ARTICLE_QUERY = "UPDATE articles SET cluster_id = %s WHERE article_id = ANY(%s::bigint[])"

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", 5432)),
    "user": os.getenv("DB_USER", "admin"),
    "password": os.getenv("DB_PASSWORD", "admin"),
    "dbname": os.getenv("DB_NAME", "news_monitor_db")
}

# indexing parameters
BATCH_SIZE = 100
K_NEIGHBOURS = 10
THRESHOLD = 0.65


def fetch_articles_batch(batch_size: int) -> Generator[List[ArticleRow], None, None]:
    """Yield batches of unclustered articles from the database.

    Args:
        batch_size: Number of articles to fetch per batch.

    Yields:
        Lists of article rows (tuples of ID, title, summary, content).
    """
    offset = 0
    while True:
        with psycopg2.connect(**DB_CONFIG) as conn:
            with conn.cursor() as cursor:
                cursor.execute(FETCH_QUERY, (batch_size, offset))
                rows = cursor.fetchall()
                if not rows:
                    break
                yield rows
                offset += batch_size
    cursor.close()
    conn.close()


def embed_batch(articles: List[ArticleRow]) -> Tuple[List[int], np.ndarray]:
    """Embed a batch of articles using the SentenceTransformer model.

    Args:
        articles: List of article tuples to embed.

    Returns:
        A tuple of:
            - list of article IDs
            - numpy array of corresponding embeddings
    """
    texts = [
        f"{title or ''} {summary or ''} {content or ''}".strip()
        for _, title, summary, content in articles
    ]

    textEmb = TextEmbedder.get_instance()
    if textEmb is not None:
        embeddings = textEmb.embed(texts)
    else:
        raise Exception("this should not have happened!")

    ids = [article[0] for article in articles]
    return ids, np.array(embeddings, dtype=np.float32)


def cluster_embeddings(ids: List[int], embeddings: np.ndarray, threshold: float = 0.8, k_neighbours: int = 10) -> Dict[int, List[int]]:
    """Cluster embeddings based on cosine similarity using FAISS.

    Args:
        ids: List of article IDs.
        embeddings: Matrix of article embeddings.
        threshold: Minimum similarity score to consider two articles in the same cluster.
        k_neighbours: Number of neighbors to query per embedding.

    Returns:
        A dictionary mapping cluster ID to list of article IDs in that cluster.
    """
    embeddings = embeddings.astype(np.float32)

    dim = embeddings.shape[1]
    index = faiss.IndexFlatIP(dim)
    index.add(embeddings)  # type: ignore

    visited = np.zeros(len(embeddings), dtype=bool)
    cluster_id = 0
    clusters: Dict[int, List[int]] = {}

    for i in range(len(embeddings)):
        if visited[i]:
            continue

        sims, neighbor_indices = index.search(embeddings[i:i+1], k_neighbours)  # type: ignore
        current_cluster = []

        for j, sim in zip(neighbor_indices[0], sims[0]):
            if sim >= threshold and not visited[j]:
                current_cluster.append(ids[j])
                visited[j] = True

        if current_cluster:
            clusters[cluster_id] = current_cluster
            cluster_id += 1

    return clusters


def create_clusters(batch_size: int, threshold: float, k: int) -> Dict[int, List[int]]:
    """Embed and cluster all unclustered articles from the database.

    Args:
        batch_size: Number of articles per batch.
        threshold: Similarity threshold for clustering.
        k: Number of nearest neighbors used in clustering.

    Returns:
        A dictionary mapping cluster ID to article ID list.
    """
    ids: List[int] = []
    embeddings: List[np.ndarray] = []

    for batch in fetch_articles_batch(batch_size):
        _ids, _embeddings = embed_batch(batch)
        ids.extend(_ids)
        embeddings.extend(_embeddings)

    embedding_matrix = np.vstack(embeddings).astype(np.float32)
    return cluster_embeddings(ids, embedding_matrix, threshold, k)


def save_clusters(clusters: Dict[int, List[int]]):
    """Persist clusters in the database and assign articles to them.

    Args:
        clusters: Dictionary mapping cluster ID to article IDs.
    """
    conn = psycopg2.connect(**DB_CONFIG)

    try:
        with conn:
            with conn.cursor() as cursor:
                for cluster_articles in clusters.values():
                    if not cluster_articles or len(cluster_articles) == 1:
                        continue
                    cursor.execute(INSERT_CLUSTER_QUERY)
                    cluster_id = cursor.fetchone()[0]
                    cursor.execute(UPDATE_ARTICLE_QUERY, (cluster_id, cluster_articles))
    except Exception as e:
        print("Error saving clusters:", e)
        conn.rollback()
    finally:
        conn.close()


def create_and_save_clusters():
    """Run the full clustering pipeline: fetch, embed, cluster, and save."""
    clusters = create_clusters(BATCH_SIZE, THRESHOLD, K_NEIGHBOURS)
    save_clusters(clusters)
