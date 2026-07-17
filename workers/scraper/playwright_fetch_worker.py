"""
playwright_fetch_worker.py

Standalone subprocess entry point for fetching a single URL via headless
Chromium. Never imported into the main scraper worker process — always run
as `python -m scraper.playwright_fetch_worker <url> <user_agent>
<load_wait_ms> <timeout_ms>` via subprocess.run() from PlaywrightFetcher.

Why a subprocess: modern pika wraps its "blocking" connection around an
asyncio-based transport internally (see pika.adapters.utils.io_services_utils),
so a long-running scraper worker already has an asyncio event loop alive in
its process. Playwright's sync API spins up its own background thread with
its own asyncio loop and depends on global asyncio/child-process-watcher
state — two independent asyncio setups sharing a process is a known-fragile
combination and was observed to break Playwright's internal dispatcher after
the RabbitMQ consumer loop had been running for a while (intermittent
'PlaywrightContextManager' object has no attribute '_playwright' /
'Connection closed while reading from the driver' errors that a fresh
in-process browser restart did not fix). Running Playwright in its own OS
process guarantees no shared asyncio/threading state, at the cost of paying
a fresh browser-launch per JS-rendered fetch — acceptable since fetches are
already rate-limited between articles.

Prints one line of JSON to stdout: {"html": "..."} on success,
{"error": "..."} on failure. Exit code 0 on success, 1 on failure.
"""
from __future__ import annotations

import json
import sys

from playwright.sync_api import sync_playwright


def fetch(url: str, user_agent: str, load_wait_ms: int, timeout_ms: float) -> str:
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--disable-http2"])
        try:
            context = browser.new_context(user_agent=user_agent)
            try:
                page = context.new_page()
                page.goto(url, timeout=timeout_ms, wait_until="domcontentloaded")
                page.wait_for_timeout(load_wait_ms)
                return str(page.content())
            finally:
                context.close()
        finally:
            browser.close()


def main() -> None:
    url, user_agent, load_wait_ms, timeout_ms = sys.argv[1:5]
    try:
        html = fetch(url, user_agent, int(load_wait_ms), float(timeout_ms))
        print(json.dumps({"html": html}))
    except Exception as exc:
        print(json.dumps({"error": str(exc)}))
        sys.exit(1)


if __name__ == "__main__":
    main()
