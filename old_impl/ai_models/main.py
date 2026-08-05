from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel
from typing import Optional
import time

from prometheus_client import (
    Counter,
    Histogram,
    generate_latest,
    CONTENT_TYPE_LATEST,
)

from classifier import NewsClassifier
from clustering import create_and_save_clusters

app = FastAPI()
classifier = NewsClassifier.get_instance()

# prometheus metrics
HTTP_REQUESTS_TOTAL = Counter(
    "http_requests_total",
    "Total HTTP requests",
    ["method", "path", "status"],
)

HTTP_REQUEST_DURATION_SECONDS = Histogram(
    "http_request_duration_seconds",
    "HTTP request duration in seconds",
    ["method", "path"],
)

CLASSIFY_TOTAL = Counter(
    "ai_classify_total",
    "Total classify calls",
    ["outcome"],
)

CLUSTER_TOTAL = Counter(
    "ai_cluster_total",
    "Total cluster calls",
    ["outcome"],
)

# per-request metrics
@app.middleware("http")
async def prometheus_middleware(request: Request, call_next):
    start = time.perf_counter()
    status_code = 500
    try:
        response = await call_next(request)
        status_code = response.status_code
        return response
    finally:
        elapsed = time.perf_counter() - start
        path = request.url.path
        method = request.method

        HTTP_REQUEST_DURATION_SECONDS.labels(method=method, path=path).observe(elapsed)
        HTTP_REQUESTS_TOTAL.labels(method=method, path=path, status=str(status_code)).inc()


# prometheus scrape endpoint
@app.get("/metrics")
def metrics():
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


class ClassifyRequest(BaseModel):
    text: Optional[str]
    default_topic: Optional[str]


class ClassifyResponse(BaseModel):
    topic: Optional[str]


@app.post("/classify", response_model=ClassifyResponse)
def classify(req: ClassifyRequest) -> ClassifyResponse:
    try:
        topic = classifier.classify(req.text)  # type: ignore
        CLASSIFY_TOTAL.labels(outcome="success").inc()
        return ClassifyResponse(topic=topic)
    except Exception:
        CLASSIFY_TOTAL.labels(outcome="fallback").inc()
        return ClassifyResponse(topic=req.default_topic)


@app.post("/cluster")
def cluster():
    try:
        create_and_save_clusters()
        CLUSTER_TOTAL.labels(outcome="success").inc()
        return {"status": "ok"}
    except Exception as e:
        CLUSTER_TOTAL.labels(outcome="error").inc()
        raise HTTPException(status_code=500, detail=str(e))
