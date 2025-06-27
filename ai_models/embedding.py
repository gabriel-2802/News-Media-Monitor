from __future__ import annotations
from typing import Optional
from sentence_transformers import SentenceTransformer
from typing import List, Union
import numpy as np  # for type hints

EMBEDDING_MODEL_NAME: str = "sentence-transformers/all-MiniLM-L6-v2"

class TextEmbedder:
    _instance: Optional[TextEmbedder] = None

    @staticmethod
    def get_instance() -> Optional[TextEmbedder]:
        if TextEmbedder._instance is None:
            TextEmbedder()
        return TextEmbedder._instance

    def __init__(self) -> None:
        if TextEmbedder._instance is not None:
            raise Exception("This class is a singleton! Use get_instance() instead.")

        self.model: SentenceTransformer = SentenceTransformer(EMBEDDING_MODEL_NAME)
        TextEmbedder._instance = self

    def embed(self, text: Union[str, List[str]]) -> Union[List[float], List[List[float]]]:
        """
        Generate context-aware embeddings. Input can be a single string or a list of strings.
        Returns:
        - A single 384-dimensional vector for one string.
        - A list of 384-dimensional vectors for multiple strings.
        """
        embeddings: np.ndarray = self.model.encode(text, normalize_embeddings=True)
        return embeddings.tolist()
