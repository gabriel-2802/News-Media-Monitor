# syntax=docker/dockerfile:1
#
# Build context is workers/ (not workers/classifier/) — this worker imports
# sibling modules (env_config.py, provider_client.py, retry.py, log_config.py)
# that live one level up, same as running `python -m classifier.classifier_worker`
# from workers/ locally.

FROM python:3.11-slim AS build
WORKDIR /build

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY classifier/requirements.txt classifier/requirements.txt
# Install the CPU-only torch build first (matching the version range pinned
# in requirements.txt below) — otherwise pip resolves the default CUDA
# build, pulling in gigabytes of unused nvidia-* wheels for a container with
# no GPU access. Once installed, the requirements.txt install below is a
# no-op for torch since the constraint is already satisfied.
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu "torch>=2.0.0,<2.2" \
    && pip install --no-cache-dir -r classifier/requirements.txt

FROM python:3.11-slim AS runtime
ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONUNBUFFERED=1

COPY --from=build /opt/venv /opt/venv
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser

USER appuser
WORKDIR /app/workers

COPY --chown=appuser:appuser env_config.py log_config.py provider_client.py retry.py ./
# Includes cls_model_output/ — the fine-tuned classifier weights are
# committed to the repo and loaded from a local path, never downloaded.
COPY --chown=appuser:appuser classifier/ ./classifier/

CMD ["python", "-m", "classifier.classifier_worker"]
