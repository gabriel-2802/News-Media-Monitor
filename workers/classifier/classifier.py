from __future__ import annotations
from pathlib import Path
from typing import Optional
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch
import json

DIRECTORY = Path(__file__).resolve().parent / "cls_model_output"

class NewsClassifier:
    """singleton classifier for news topic classification.

    ensures that only one instance of the model and tokenizer is loaded.
    results showed an increase of over 30% in accuracy compared to a LogisticRegression model (86% vs 52%).
    """

    _instance: Optional[NewsClassifier] = None

    @staticmethod
    def get_instance():
        """Return the singleton instance of the NewsClassifier.

        If the instance does not yet exist, it is created automatically.
        """
        if NewsClassifier._instance is None:
            NewsClassifier()
        return NewsClassifier._instance

    def __init__(self):
        """Initialize the NewsClassifier by loading the model and tokenizer.

        Raises:
            Exception: if an instance already exists (enforcing singleton pattern).
        """
        if NewsClassifier._instance is not None:
            raise Exception("This class is a singleton! Use get_instance() instead.")
        
        model_dir = DIRECTORY
        self.tokenizer = AutoTokenizer.from_pretrained(model_dir)
        self.model = AutoModelForSequenceClassification.from_pretrained(model_dir)
        self.model.eval()

        with open(f"{model_dir}/label2id.json", "r") as f:
            label2id = json.load(f)
        self.id2label = {int(v): k for k, v in label2id.items()}

        NewsClassifier._instance = self

    def classify(self, text: str) -> str:
        """Classify a given input string into a topic label.

        Args:
            text: The input text to classify.

        Returns:
            The predicted topic label as a string.
        """
        inputs = self.tokenizer(text, return_tensors="pt", truncation=True, padding=True, max_length=512)
        with torch.no_grad():
            logits = self.model(**inputs).logits
        pred_id = int(torch.argmax(logits, dim=1).item())
        return self.id2label[pred_id]
