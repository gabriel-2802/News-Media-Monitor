import pandas as pd
from datasets import Dataset, DatasetDict
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from transformers.training_args import TrainingArguments
from transformers.trainer import Trainer
from transformers.modeling_utils import PreTrainedModel
from typing import Tuple, Dict
from sklearn.metrics import accuracy_score
import numpy as np
import json

MODEL_NAME: str = "distilbert-base-uncased"
TOKENIZER = AutoTokenizer.from_pretrained(MODEL_NAME)

def load_dataset(file_path: str) -> Tuple[Dataset, Dict[str, int], Dict[int, str]]:
    """Load and preprocess the dataset from a JSON file.

    Args:
        file_path: Path to the JSONL dataset file.

    Returns:
        A tuple containing:
        - HuggingFace Dataset
        - label-to-id dictionary
        - id-to-label dictionary
    """
    df: pd.DataFrame = pd.read_json(file_path, lines=True)
    df["text"] = df["headline"] + " " + df["short_description"]
    df = df[["text", "category"]]

    df["category"] = df["category"].astype(str).str.replace(r"[^a-zA-Z\s]", "", regex=True).str.strip().str.lower()
    df = df.dropna(subset=["text", "category"])

    label_to_id: Dict[str, int] = {label: idx for idx, label in enumerate(sorted(df["category"].unique()))}
    id_to_label: Dict[int, str] = {idx: label for label, idx in label_to_id.items()}

    df["label"] = df["category"].map(label_to_id)
    df = df.drop(columns=["category"])

    return Dataset.from_pandas(df[["text", "label"]]), label_to_id, id_to_label

def tokenize(batch: Dict[str, list]) -> Dict[str, list]:
    """Tokenize a batch of text entries using the global tokenizer.

    Args:
        batch: A dictionary with a "text" key containing a list of strings.

    Returns:
        A dictionary of tokenized inputs.
    """
    return TOKENIZER(batch["text"], padding="max_length", truncation=True, max_length=512)

def compute_metrics(eval_pred):
    """Compute classification accuracy from model predictions.

    Args:
        eval_pred: A tuple (logits, labels) from evaluation step.

    Returns:
        A dictionary with an "accuracy" key and its corresponding score.
    """
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=-1)
    return {"accuracy": accuracy_score(labels, preds)}

def train_model(dataset: DatasetDict, model: PreTrainedModel) -> None:
    """Train the classification model and save it to disk.

    Args:
        dataset: A DatasetDict containing "train" and "test" splits.
        model: A pre-initialized HuggingFace classification model.
    """
    training_args = TrainingArguments(
        output_dir="./model_output",
        num_train_epochs=2,
        per_device_train_batch_size=8,
        per_device_eval_batch_size=8,
        save_strategy="epoch",
        logging_dir="./logs",
        logging_steps=200,
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=dataset["train"],
        eval_dataset=dataset["test"],
        compute_metrics=compute_metrics,
    )

    trainer.train()

    model.save_pretrained("./model_output")
    TOKENIZER.save_pretrained("./model_output")

    with open("./model_output/label2id.json", "w") as f:
        json.dump(model.config.label2id, f)
    with open("./model_output/id2label.json", "w") as f:
        json.dump(model.config.id2label, f)

def main() -> None:
    """Run the full training pipeline."""
    dataset: Dataset
    label_to_id: Dict[str, int]
    id_to_label: Dict[int, str]

    dataset, label_to_id, id_to_label = load_dataset("data/News_Category_Dataset_v3.json")

    tokenized_dataset: Dataset = dataset.map(tokenize, batched=True)
    dataset_dict: DatasetDict = tokenized_dataset.train_test_split(test_size=0.2, seed=42)

    model = AutoModelForSequenceClassification.from_pretrained(
        MODEL_NAME,
        num_labels=len(label_to_id),
        id2label=id_to_label,
        label2id=label_to_id
    )  
    model.to("cuda") 
    train_model(dataset_dict, model)

# if __name__ == "__main__":
#     main()
