# syntax=docker/dockerfile:1
#
# Build context is workers/ (not workers/clusterer/) — this worker imports
# sibling modules (env_config.py, provider_client.py, retry.py, log_config.py)
# that live one level up, same as running `python -m clusterer.clusterer_worker`
# from workers/ locally.

FROM python:3.11-slim AS build
WORKDIR /build

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY clusterer/requirements.txt clusterer/requirements.txt
# Install the CPU-only torch build first — otherwise pip resolves the
# default CUDA build, pulling in gigabytes of unused nvidia-* wheels for a
# container with no GPU access. Once installed, the requirements.txt install
# below is a no-op for torch since it's already satisfied (unpinned there).
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch \
    && pip install --no-cache-dir -r clusterer/requirements.txt

FROM python:3.11-slim AS runtime
ARG EMBEDDING_MODEL_NAME=sentence-transformers/all-MiniLM-L6-v2
ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONUNBUFFERED=1 \
    EMBEDDING_MODEL_NAME=${EMBEDDING_MODEL_NAME}

COPY --from=build /opt/venv /opt/venv
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser

USER appuser
WORKDIR /app/workers

# Pre-download the embedding model into the image (appuser's HF cache) so
# replicas start instantly with no network dependency and no shared cache
# volume needed across the 3 clusterer replicas.
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('${EMBEDDING_MODEL_NAME}')"

COPY --chown=appuser:appuser env_config.py log_config.py provider_client.py retry.py ./
COPY --chown=appuser:appuser clusterer/ ./clusterer/

CMD ["python", "-m", "clusterer.clusterer_worker"]
