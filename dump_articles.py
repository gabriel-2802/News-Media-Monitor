"""
Drain all messages from the articles queue and save them as a JSON array.
Messages are acked (removed from the queue) as they are read.

Usage:
    python dump_articles.py [output_file]   # default: articles_dump.json
"""
from __future__ import annotations

import json
import os
import sys

import pika

RABBITMQ_URL  = os.getenv("RABBITMQ_URL", "amqp://admin:secret@localhost:5672/news_monitor")
ARTICLES_QUEUE = "articles"
OUTPUT_PATH    = sys.argv[1] if len(sys.argv) > 1 else "articles_dump.json"

connection = pika.BlockingConnection(pika.URLParameters(RABBITMQ_URL))
channel    = connection.channel()

articles: list[dict] = []

while True:
    method, _props, body = channel.basic_get(queue=ARTICLES_QUEUE, auto_ack=True)
    if method is None:
        break
    articles.append(json.loads(body))

connection.close()

with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
    json.dump(articles, f, ensure_ascii=False, indent=2)

print(f"Saved {len(articles)} articles → {OUTPUT_PATH}")
