from __future__ import annotations
from typing import Optional
from sentence_transformers import SentenceTransformer
from typing import List, Union
import numpy as np  # for type hints

EMBEDDING_MODEL_NAME: str = "sentence-transformers/all-MiniLM-L6-v2"


class TextEmbedder:
    """singleton class for generating sentence embeddings using SentenceTransformer."""

    _instance: Optional[TextEmbedder] = None

    @staticmethod
    def get_instance() -> Optional[TextEmbedder]:
        """Return the singleton instance of the TextEmbedder.

        Returns:
            The singleton instance of the TextEmbedder class.
        """
        if TextEmbedder._instance is None:
            TextEmbedder()
        return TextEmbedder._instance

    def __init__(self) -> None:
        """Initialize the embedding model.

        Loads the SentenceTransformer model and enforces the singleton pattern.

        Raises:
            Exception: If an instance already exists.
        """
        if TextEmbedder._instance is not None:
            raise Exception("This class is a singleton! Use get_instance() instead.")

        self.model: SentenceTransformer = SentenceTransformer(EMBEDDING_MODEL_NAME)
        TextEmbedder._instance = self

    def embed(self, text: Union[str, List[str]]) -> Union[List[float], List[List[float]]]:
        """Generate normalized embeddings from input text.

        Args:
            text: A string or list of strings to embed.

        Returns:
            A single 384-dimensional embedding (for one string) or a list of embeddings (for multiple strings),
            each as a list of floats.
        """
        embeddings: np.ndarray = self.model.encode(text, normalize_embeddings=True)
        return embeddings.tolist()
