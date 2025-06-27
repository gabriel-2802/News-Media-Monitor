from __future__ import annotations
from typing import Optional
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch
import json

DIRECTORY = "./cls_model_output"

"""
    singleton pattern to ensure only one instance of the classifier is created
    results showed an increase of over 30% in accuracy compared to a LogisticRegression Model (86% compared to 52%)
"""
class NewsClassifier:
    _instance: Optional[NewsClassifier] = None

    @staticmethod
    def get_instance():
        if NewsClassifier._instance is None:
            NewsClassifier()
        return NewsClassifier._instance

    def __init__(self):
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
        inputs = self.tokenizer(text, return_tensors="pt", truncation=True, padding=True, max_length=512)
        with torch.no_grad():
            logits = self.model(**inputs).logits
        pred_id = int(torch.argmax(logits, dim=1).item())
        return self.id2label[pred_id]
