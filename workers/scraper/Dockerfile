# syntax=docker/dockerfile:1
#
# Build context is workers/ (not workers/scraper/) — this worker imports
# sibling modules (env_config.py, provider_client.py, retry.py, log_config.py)
# that live one level up, same as running `python -m scraper.scraper_worker`
# from workers/ locally.

FROM python:3.11-slim AS build
WORKDIR /build

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY scraper/requirements.txt scraper/requirements.txt
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r scraper/requirements.txt lxml_html_clean charset-normalizer

FROM python:3.11-slim AS runtime
ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONUNBUFFERED=1

COPY --from=build /opt/venv /opt/venv

# OS libraries Chromium needs at runtime — root-only step (apt).
RUN python -m playwright install-deps chromium \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --shell /usr/sbin/nologin appuser

# Chromium refuses to run its own sandbox as root, so the browser binary
# must be downloaded by (and later launched as) a non-root user.
USER appuser
WORKDIR /app/workers
RUN python -m playwright install chromium

COPY --chown=appuser:appuser env_config.py log_config.py provider_client.py retry.py ./
COPY --chown=appuser:appuser scraper/ ./scraper/

CMD ["python", "-m", "scraper.scraper_worker"]
