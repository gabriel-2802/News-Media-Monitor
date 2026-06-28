"""
Smoke test: for each source, fetch RSS, parse entries, probe first article for JS.
Results are printed to stdout and saved to smoke_test_results.json.
"""
from __future__ import annotations

import json
import time
from datetime import datetime
from typing import Any

import requests
import news_scraper
from news_scraper import (
    SOURCES, USER_AGENT, BROWSER_UA, RATE_LIMIT_SECONDS,
    fetch_html, parse_rss, requires_js, FetchError,
)

# Speed up the smoke test: 1 attempt only, shorter rate limit
news_scraper.MAX_RETRIES = 1
news_scraper.RATE_LIMIT_SECONDS = 1.0

RESULTS_PATH = "smoke_test_results.json"

session = requests.Session()
session.headers["User-Agent"] = USER_AGENT

results: list[dict[str, Any]] = []

print(f"\n{'SOURCE':<14} {'RSS':^6} {'ENTRIES':^8} {'PROBE URL':<60} {'JS?':^6} {'BODY?'}")
print("-" * 110)

for src in SOURCES:
    row: dict[str, Any] = {"source": src.name, "rss_url": src.rss_url}

    # 1. fetch RSS (some sources need browser UA on the feed endpoint)
    if src.rss_browser_ua:
        session.headers["User-Agent"] = BROWSER_UA
    try:
        feed_html = fetch_html(src.rss_url, session)
        row["rss_ok"] = True
    except FetchError as e:
        session.headers["User-Agent"] = USER_AGENT
        row.update({"rss_ok": False, "error": str(e)})
        results.append(row)
        print(f"{src.name:<14} {'FAIL':^6}  —  {str(e)[:80]}")
        time.sleep(RATE_LIMIT_SECONDS)
        continue
    finally:
        session.headers["User-Agent"] = USER_AGENT

    # 2. parse RSS
    entries = parse_rss(feed_html)
    n = len(entries)
    row["entry_count"] = n

    if not entries:
        row.update({"rss_ok": True, "error": "no entries parsed"})
        results.append(row)
        print(f"{src.name:<14} {'ok':^6} {n:^8}  no entries found")
        time.sleep(RATE_LIMIT_SECONDS)
        continue

    first = entries[0]
    row["probe_url"] = first.url
    row["meta_title"] = bool(first.title)
    row["meta_author"] = bool(first.author)
    row["meta_date"] = bool(first.published_at)

    # 3. probe first article for JS requirement
    time.sleep(RATE_LIMIT_SECONDS)
    js = requires_js(first.url, session)
    row["js_required"] = js

    results.append(row)
    meta = f"title={row['meta_title']} author={row['meta_author']} date={row['meta_date']}"
    print(f"{src.name:<14} {'ok':^6} {n:^8} {first.url[:58]:<60} {'yes' if js else 'no':^6}  {meta}")
    time.sleep(RATE_LIMIT_SECONDS)

print()

# Save results
output = {"run_at": datetime.utcnow().isoformat(), "sources": results}
with open(RESULTS_PATH, "w", encoding="utf-8") as f:
    json.dump(output, f, indent=2)
print(f"Results saved to {RESULTS_PATH}")
