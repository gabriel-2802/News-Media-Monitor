"""
embedder.py — singleton embedding model, same pattern as NewsClassifier.
"""
from __future__ import annotations
from typing import Optional
from sentence_transformers import SentenceTransformer
import torch

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"  # see data/output/validation_report.md — 27x smaller than Qwen3-Embedding-0.6B for a 90.8%->88.0% balanced-accuracy tradeoff


class ArticleEmbedder:
    """Singleton embedding model for article/story vectors."""

    _instance: Optional["ArticleEmbedder"] = None

    @staticmethod
    def get_instance() -> "ArticleEmbedder":
        if ArticleEmbedder._instance is None:
            ArticleEmbedder()
        return ArticleEmbedder._instance

    def __init__(self) -> None:
        if ArticleEmbedder._instance is not None:
            raise Exception("This class is a singleton! Use get_instance() instead.")

        # MPS = Apple Silicon GPU backend. Falls back to CPU if unavailable.
        device = "mps" if torch.backends.mps.is_available() else "cpu"
        self.model = SentenceTransformer(MODEL_NAME, device=device)

        ArticleEmbedder._instance = self

    def embed(self, text: str) -> list[float]:
        """Generate a normalized embedding vector for a piece of text."""
        # normalize_embeddings=True → vectors are unit length, so cosine
        # similarity reduces to a dot product (cheaper, and what Qdrant's
        # cosine distance metric expects).
        vec = self.model.encode(text, normalize_embeddings=True)
        return vec.tolist()