from re import S
from fastapi import FastAPI
from pydantic import BaseModel

from classifier import NewsClassifier
from clustering import create_and_save_clusters

app = FastAPI()
classifier = NewsClassifier.get_instance()

class ClassifyRequest(BaseModel):
    text: str
    default_topic: str

class ClassifyResponse(BaseModel):
    topic : str

@app.post("/classify", response_model=ClassifyResponse)
def classify(req: ClassifyRequest) -> ClassifyResponse:
    try:
        topic = classifier.classify(req.text) # type: ignore
        return ClassifyResponse(topic=topic)
    except Exception as _:
        return ClassifyResponse(topic=req.default_topic)
    
@app.post("/cluster")
def cluster():
    create_and_save_clusters()
