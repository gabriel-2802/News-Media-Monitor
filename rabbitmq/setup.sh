#!/bin/sh
set -e

BASE="http://rabbitmq:15672/api"
AUTH="${RABBITMQ_USER}:${RABBITMQ_PASSWORD}"

echo "[setup] Waiting for management API..."
until curl -sf -u "${AUTH}" "${BASE}/overview" > /dev/null 2>&1; do
  sleep 2
done

echo "[setup] Creating vhost: news_monitor"
curl -sf -u "${AUTH}" -X PUT "${BASE}/vhosts/news_monitor"

echo "[setup] Setting permissions for ${RABBITMQ_USER} on news_monitor"
curl -sf -u "${AUTH}" -X PUT "${BASE}/permissions/news_monitor/${RABBITMQ_USER}" \
  -H "Content-Type: application/json" \
  -d '{"configure":".*","write":".*","read":".*"}'

echo "[setup] Creating exchange: news_monitor.dlx (fanout dead-letter exchange)"
curl -sf -u "${AUTH}" -X PUT "${BASE}/exchanges/news_monitor/news_monitor.dlx" \
  -H "Content-Type: application/json" \
  -d '{"type":"fanout","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'

echo "[setup] Creating exchange: news_monitor (topic)"
curl -sf -u "${AUTH}" -X PUT "${BASE}/exchanges/news_monitor/news_monitor" \
  -H "Content-Type: application/json" \
  -d '{"type":"topic","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'

echo "[setup] Creating queue: scrape.jobs.dead"
curl -sf -u "${AUTH}" -X PUT "${BASE}/queues/news_monitor/scrape.jobs.dead" \
  -H "Content-Type: application/json" \
  -d '{"durable":true,"auto_delete":false,"arguments":{}}'

echo "[setup] Creating queue: scrape.jobs (DLX + 24h TTL)"
curl -sf -u "${AUTH}" -X PUT "${BASE}/queues/news_monitor/scrape.jobs" \
  -H "Content-Type: application/json" \
  -d '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"news_monitor.dlx","x-message-ttl":86400000}}'

echo "[setup] Binding scrape.jobs.dead -> news_monitor.dlx"
curl -sf -u "${AUTH}" -X POST "${BASE}/bindings/news_monitor/e/news_monitor.dlx/q/scrape.jobs.dead" \
  -H "Content-Type: application/json" \
  -d '{"routing_key":"#","arguments":{}}'

echo "[setup] Binding scrape.jobs -> news_monitor (routing_key: scrape.job)"
curl -sf -u "${AUTH}" -X POST "${BASE}/bindings/news_monitor/e/news_monitor/q/scrape.jobs" \
  -H "Content-Type: application/json" \
  -d '{"routing_key":"scrape.job","arguments":{}}'

echo "[setup] Creating queue: clustering"
curl -sf -u "${AUTH}" -X PUT "${BASE}/queues/news_monitor/clustering" \
  -H "Content-Type: application/json" \
  -d '{"durable":true,"auto_delete":false,"arguments":{}}'

echo "[setup] Binding clustering -> news_monitor (routing_key: article.clustering)"
curl -sf -u "${AUTH}" -X POST "${BASE}/bindings/news_monitor/e/news_monitor/q/clustering" \
  -H "Content-Type: application/json" \
  -d '{"routing_key":"article.clustering","arguments":{}}'

echo "[setup] RabbitMQ topology is ready."
