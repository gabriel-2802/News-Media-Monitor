from re import S
from fastapi import FastAPI
from pydantic import BaseModel
from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError

from typing import Optional
from classifier import NewsClassifier
from clustering import create_and_save_clusters

app = FastAPI()
classifier = NewsClassifier.get_instance()


class ClassifyRequest(BaseModel):
    """Request model for the /classify endpoint.

    Attributes:
        text: The article or input string to classify.
        default_topic: Fallback topic in case classification fails.
    """
    text: Optional[str]
    default_topic: Optional[str]


class ClassifyResponse(BaseModel):
    """Response model for the /classify endpoint.

    Attributes:
        topic: Predicted or fallback topic.
    """
    topic: Optional[str]

@app.post("/classify", response_model=ClassifyResponse)
def classify(req: ClassifyRequest) -> ClassifyResponse:
    """Classify input text into a topic using the NewsClassifier.

    Args:
        req: Request payload containing the text and default topic.

    Returns:
        ClassifyResponse with either the predicted or fallback topic.
    """
    try:
        topic = classifier.classify(req.text)  # type: ignore
        return ClassifyResponse(topic=topic)
    except Exception as _:
        return ClassifyResponse(topic=req.default_topic)



@app.post("/cluster")
def cluster():
    """Trigger article clustering and persist results in the database."""
    create_and_save_clusters()
