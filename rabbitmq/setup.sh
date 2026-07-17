#!/bin/sh
set -e

BASE="http://localhost:15672/api"
AUTH="${RABBITMQ_USER:-admin}:${RABBITMQ_PASSWORD:-secret}"

echo "[setup] Waiting for management API..."
RETRY=0
MAX_RETRIES=30
until [ $RETRY -eq $MAX_RETRIES ]; do
  if curl -s -u "${AUTH}" "${BASE}/overview" > /dev/null 2>&1; then
    echo "[setup] Management API is ready"
    break
  fi
  RETRY=$((RETRY + 1))
  echo "[setup] Attempt $RETRY/$MAX_RETRIES..."
  sleep 2
done

if [ $RETRY -eq $MAX_RETRIES ]; then
  echo "[setup] ERROR: Management API never became ready"
  exit 1
fi

echo "[setup] Creating vhost: news_monitor"
curl -s -u "${AUTH}" -X PUT "${BASE}/vhosts/news_monitor"
echo ""

echo "[setup] Setting permissions for ${RABBITMQ_USER:-admin} on news_monitor"
curl -s -u "${AUTH}" -X PUT "${BASE}/permissions/news_monitor/${RABBITMQ_USER:-admin}" \
-H "Content-Type: application/json" \
-d '{"configure":".*","write":".*","read":".*"}'
echo ""

echo "[setup] Creating exchange: news_monitor.dlx (fanout dead-letter exchange)"
curl -s -u "${AUTH}" -X PUT "${BASE}/exchanges/news_monitor/news_monitor.dlx" \
-H "Content-Type: application/json" \
-d '{"type":"fanout","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'
echo ""

echo "[setup] Creating exchange: news_monitor (topic)"
curl -s -u "${AUTH}" -X PUT "${BASE}/exchanges/news_monitor/news_monitor" \
-H "Content-Type: application/json" \
-d '{"type":"topic","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'
echo ""

echo "[setup] Creating queue: scrape.jobs.dead"
curl -s -u "${AUTH}" -X PUT "${BASE}/queues/news_monitor/scrape.jobs.dead" \
-H "Content-Type: application/json" \
-d '{"durable":true,"auto_delete":false,"arguments":{}}'
echo ""

echo "[setup] Creating queue: scrape.jobs (DLX + 24h TTL)"
curl -s -u "${AUTH}" -X PUT "${BASE}/queues/news_monitor/scrape.jobs" \
-H "Content-Type: application/json" \
-d '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"news_monitor.dlx","x-message-ttl":86400000}}'
echo ""

echo "[setup] Binding scrape.jobs.dead -> news_monitor.dlx"
curl -s -u "${AUTH}" -X POST "${BASE}/bindings/news_monitor/e/news_monitor.dlx/q/scrape.jobs.dead" \
-H "Content-Type: application/json" \
-d '{"routing_key":"#","arguments":{}}'
echo ""

echo "[setup] Binding scrape.jobs -> news_monitor (routing_key: scrape.job)"
curl -s -u "${AUTH}" -X POST "${BASE}/bindings/news_monitor/e/news_monitor/q/scrape.jobs" \
-H "Content-Type: application/json" \
-d '{"routing_key":"scrape.job","arguments":{}}'
echo ""

echo "[setup] Creating queue: clustering"
curl -s -u "${AUTH}" -X PUT "${BASE}/queues/news_monitor/clustering" \
-H "Content-Type: application/json" \
-d '{"durable":true,"auto_delete":false,"arguments":{}}'
echo ""

echo "[setup] Binding clustering -> news_monitor (routing_key: article.clustering)"
curl -s -u "${AUTH}" -X POST "${BASE}/bindings/news_monitor/e/news_monitor/q/clustering" \
-H "Content-Type: application/json" \
-d '{"routing_key":"article.clustering","arguments":{}}'
echo ""

echo "[setup] Creating queue: embedding"
curl -s -u "${AUTH}" -X PUT "${BASE}/queues/news_monitor/embedding" \
-H "Content-Type: application/json" \
-d '{"durable":true,"auto_delete":false,"arguments":{}}'
echo ""

echo "[setup] Binding embedding -> news_monitor (routing_key: article.clustering)"
curl -s -u "${AUTH}" -X POST "${BASE}/bindings/news_monitor/e/news_monitor/q/embedding" \
-H "Content-Type: application/json" \
-d '{"routing_key":"article.clustering","arguments":{}}'
echo ""

echo "[setup] ✓ RabbitMQ topology is ready."