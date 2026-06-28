"""
News scraper POC — multi-source, RSS-first metadata, Playwright fallback for JS sites.

Architecture:
  RSS feed → metadata (title, author, date, url)
  Article page (static or JS-rendered) → body text only
"""

from __future__ import annotations

import json
import logging
import time
import urllib.robotparser
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from typing import Optional
from urllib.parse import urljoin, urlparse

import requests
import trafilatura
from bs4 import BeautifulSoup
from dateutil import parser as dateutil_parser
from playwright.sync_api import sync_playwright

# ---------------------------------------------------------------------------
# Source config — add more sources here
# ---------------------------------------------------------------------------

@dataclass
class SourceConfig:
    name: str
    base_url: str
    rss_url: str
    # Use browser UA when fetching RSS (for sites that block bot UAs on feeds)
    rss_browser_ua: bool = False


# SOURCES: list[SourceConfig] = [
#     SourceConfig(name="bbc",         base_url="https://www.bbc.co.uk",         rss_url="https://feeds.bbci.co.uk/news/world/rss.xml"),
#     SourceConfig(name="guardian",    base_url="https://www.theguardian.com",    rss_url="https://www.theguardian.com/world/rss"),
#     SourceConfig(name="aljazeera",   base_url="https://www.aljazeera.com",      rss_url="https://www.aljazeera.com/xml/rss/all.xml"),
#     SourceConfig(name="npr",         base_url="https://www.npr.org",            rss_url="https://feeds.npr.org/1001/rss.xml"),
#     SourceConfig(name="dw",          base_url="https://www.dw.com",             rss_url="https://rss.dw.com/rdf/rss-en-all"),
#     SourceConfig(name="france24",    base_url="https://www.france24.com",       rss_url="https://www.france24.com/en/rss"),
#     SourceConfig(name="cbc",         base_url="https://www.cbc.ca",             rss_url="https://www.cbc.ca/cmlink/rss-world",        rss_browser_ua=True),
#     SourceConfig(name="abc_au",      base_url="https://www.abc.net.au",         rss_url="https://www.abc.net.au/news/feed/51120/rss.xml"),
#     SourceConfig(name="euronews",    base_url="https://www.euronews.com",       rss_url="https://www.euronews.com/rss"),
#     SourceConfig(name="sky",         base_url="https://news.sky.com",           rss_url="https://feeds.skynews.com/feeds/rss/world.xml"),
#     SourceConfig(name="independent", base_url="https://www.independent.co.uk",  rss_url="https://www.independent.co.uk/news/world/rss"),
#     SourceConfig(name="thehill",     base_url="https://thehill.com",            rss_url="https://thehill.com/feed"),
#     # Politico: Cloudflare blocks RSS endpoint — omitted
#     SourceConfig(name="vox",         base_url="https://www.vox.com",            rss_url="https://www.vox.com/rss/index.xml"),
#     SourceConfig(name="time",        base_url="https://time.com",               rss_url="https://time.com/feed"),
#     SourceConfig(name="rt",          base_url="https://www.rt.com",             rss_url="https://www.rt.com/rss/news"),
# ]
# Minimum body-text length (chars) to consider a static fetch successful
JS_DETECTION_MIN_CHARS = 200

OUTPUT_PATH = "articles.jsonl"
USER_AGENT = (
    "NewsMonitorBot/0.1 (research scraper; contact: gabrielvalentine738@gmail.com)"
)
# User-agent for headless browser (realistic browser UA avoids most bot blocks)
BROWSER_UA = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)
# (connect timeout, read timeout) — prevents slow servers from blocking forever
REQUEST_TIMEOUT = (5, 10)
RATE_LIMIT_SECONDS = 2.0
MAX_RETRIES = 3
RETRY_BACKOFF = 3.0
# Wait for JS page to settle (ms)
JS_LOAD_WAIT_MS = 3000

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------


@dataclass
class RSSEntry:
    url: str
    title: str
    author: Optional[str]
    published_at: Optional[datetime]


@dataclass
class Article:
    url: str
    title: str
    author: Optional[str]
    published_at: Optional[datetime]
    body_text: str
    scraped_at: datetime
    source: str


class FetchError(Exception):
    pass


class ExtractionError(Exception):
    pass


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _parse_date(date_str: Optional[str]) -> Optional[datetime]:
    if not date_str:
        return None
    try:
        dt: datetime = dateutil_parser.parse(date_str)
        return dt
    except (ValueError, OverflowError):
        return None


def _text(tag: object) -> Optional[str]:
    """Return stripped text from a BS4 tag, or None."""
    if tag is None:
        return None
    s = getattr(tag, "string", None) or getattr(tag, "get_text", lambda: None)()
    return s.strip() if isinstance(s, str) and s.strip() else None


# ---------------------------------------------------------------------------
# Robots.txt
# ---------------------------------------------------------------------------


def build_robot_parser(base_url: str) -> urllib.robotparser.RobotFileParser:
    rp = urllib.robotparser.RobotFileParser()
    rp.set_url(urljoin(base_url, "/robots.txt"))
    try:
        rp.read()
    except Exception as exc:
        log.warning("Could not read robots.txt for %s: %s", base_url, exc)
    return rp


# ---------------------------------------------------------------------------
# Fetcher (static)
# ---------------------------------------------------------------------------


def fetch_html(url: str, session: requests.Session) -> str:
    last_exc: Exception = RuntimeError("no attempts made")
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            response = session.get(url, timeout=REQUEST_TIMEOUT)
            response.raise_for_status()
            text: str = response.text
            return text
        except requests.HTTPError as exc:
            if exc.response is not None and exc.response.status_code in (404, 410):
                raise FetchError(f"Permanent HTTP error for {url}: {exc}") from exc
            last_exc = exc
        except requests.RequestException as exc:
            last_exc = exc
        if attempt < MAX_RETRIES:
            sleep_time = RETRY_BACKOFF * attempt
            log.debug("Retry %d/%d for %s in %.1fs", attempt, MAX_RETRIES, url, sleep_time)
            time.sleep(sleep_time)
    raise FetchError(
        f"Failed to fetch {url} after {MAX_RETRIES} attempts: {last_exc}"
    ) from last_exc


# ---------------------------------------------------------------------------
# Fetcher (JS / Playwright)
# ---------------------------------------------------------------------------


def fetch_html_js(url: str) -> str:
    """Render a page with a headless browser and return the final HTML."""
    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True)
        ctx = browser.new_context(user_agent=BROWSER_UA)
        page = ctx.new_page()
        try:
            # Playwright timeout is a plain float (ms); use the read part of our tuple
            pw_timeout = float(REQUEST_TIMEOUT[1] * 1000)
            page.goto(url, timeout=pw_timeout, wait_until="domcontentloaded")
            page.wait_for_timeout(JS_LOAD_WAIT_MS)
            html: str = page.content()
        finally:
            browser.close()
    return html


# ---------------------------------------------------------------------------
# JS detection
# ---------------------------------------------------------------------------


def requires_js(url: str, session: requests.Session) -> bool:
    """Return True if a static fetch of url yields too little body text."""
    try:
        html = fetch_html(url, session)
    except FetchError:
        # Can't reach it statically — assume JS needed
        return True
    try:
        body = extract_body(html, url)
        result = len(body) >= JS_DETECTION_MIN_CHARS
        log.info(
            "JS detection for %s: static body=%d chars → js_required=%s",
            url, len(body), not result,
        )
        return not result
    except ExtractionError:
        log.info("JS detection for %s: no body extracted → js_required=True", url)
        return True


# ---------------------------------------------------------------------------
# RSS parsing — source of metadata
# ---------------------------------------------------------------------------


def parse_rss(feed_html: str) -> list[RSSEntry]:
    """Extract article metadata from RSS 2.0 or Atom feeds."""
    soup = BeautifulSoup(feed_html, "lxml-xml")
    entries: list[RSSEntry] = []
    seen: set[str] = set()

    # Detect feed format by presence of <item> (RSS) vs <entry> (Atom)
    rss_items = soup.find_all("item")
    atom_entries = soup.find_all("entry")
    items = rss_items if rss_items else atom_entries
    is_atom = not rss_items and bool(atom_entries)

    for item in items:
        url: Optional[str] = None

        if is_atom:
            # Atom: <link rel="alternate" href="..."/> or <link href="..."/>
            for link_tag in item.find_all("link"):
                rel = link_tag.get("rel", "alternate")
                href = link_tag.get("href", "")
                if href and rel in ("alternate", ""):
                    url = str(href)
                    break
            if not url:
                id_tag = item.find("id")
                if id_tag:
                    url = _text(id_tag)
        else:
            # RSS 2.0: <link> text node, fallback to <guid>
            rss_link = item.find("link")
            if rss_link is not None:
                url = _text(rss_link) or str(rss_link.get("href", "") or "")
            if not url:
                guid = item.find("guid")
                if guid:
                    url = _text(guid)

        if not url:
            continue
        url = url.split("?")[0]  # strip tracking params
        if url in seen:
            continue
        seen.add(url)

        title: str = _text(item.find("title")) or ""

        if is_atom:
            author_tag = item.find("author")
            name_tag = author_tag.find("name") if author_tag else None
            author_name = _text(name_tag) if name_tag else None
            author: Optional[str] = author_name
            pub_date_str = _text(item.find("published")) or _text(item.find("updated"))
        else:
            author = _text(item.find("dc:creator")) or _text(item.find("author"))
            pub_date_str = _text(item.find("pubDate")) or _text(item.find("dc:date"))

        published_at = _parse_date(pub_date_str)
        entries.append(RSSEntry(url=url, title=title, author=author, published_at=published_at))

    fmt = "Atom" if is_atom else "RSS"
    log.info("Parsed %d entries from %s feed", len(entries), fmt)
    return entries


# ---------------------------------------------------------------------------
# Body extraction — article page only
# ---------------------------------------------------------------------------


def extract_body(html: str, url: str) -> str:
    """Extract article body text from HTML using trafilatura."""
    raw = trafilatura.bare_extraction(
        html,
        url=url,
        include_comments=False,
        include_tables=False,
        as_dict=True,
    )
    if raw is None:
        raise ExtractionError(f"trafilatura returned no content for {url}")
    result: dict[str, object] = raw  # type: ignore[assignment]
    body: str = str(result.get("text") or "")
    if not body:
        raise ExtractionError(f"Empty body text for {url}")
    return body


# ---------------------------------------------------------------------------
# Storage
# ---------------------------------------------------------------------------


def _article_to_dict(article: Article) -> dict[str, object]:
    d = asdict(article)
    for key in ("published_at", "scraped_at"):
        val = d[key]
        if isinstance(val, datetime):
            d[key] = val.isoformat()
    return d


def save_article(article: Article, output_path: str) -> None:
    with open(output_path, "a", encoding="utf-8") as fh:
        fh.write(json.dumps(_article_to_dict(article), ensure_ascii=False) + "\n")


# ---------------------------------------------------------------------------
# Per-source scrape
# ---------------------------------------------------------------------------


def scrape_source(
    source: SourceConfig,
    session: requests.Session,
    output_path: str,
) -> tuple[int, int, int]:
    """Scrape one source. Returns (attempted, succeeded, failed)."""
    log.info("=== Scraping source: %s ===", source.name)

    rp = build_robot_parser(source.base_url)

    # Fetch RSS — some sites block bot UAs on feed endpoints
    if source.rss_browser_ua:
        session.headers["User-Agent"] = BROWSER_UA
    try:
        feed_html = fetch_html(source.rss_url, session)
    except FetchError as exc:
        log.error("Cannot fetch RSS for %s: %s", source.name, exc)
        return 0, 0, 0
    finally:
        session.headers["User-Agent"] = USER_AGENT

    time.sleep(RATE_LIMIT_SECONDS)

    entries = parse_rss(feed_html)
    if not entries:
        log.warning("No entries found in RSS for %s", source.name)
        return 0, 0, 0

    attempted = succeeded = failed = 0

    for entry in entries:
        if not rp.can_fetch(USER_AGENT, entry.url):
            log.info("robots.txt disallows %s — skipping", entry.url)
            continue

        attempted += 1

        # BUG FIX: js_required was previously detected once per *source* (on the
        # first article) and then reused for every subsequent article. Sites
        # commonly mix static and JS-rendered article pages, so that caching
        # silently misclassified later articles. Detect it per article instead.
        log.info("[%s] Probing for JS requirement: %s", source.name, entry.url)
        js_required = requires_js(entry.url, session)
        time.sleep(RATE_LIMIT_SECONDS)

        log.info("[%s %d] Fetching %s (js=%s)", source.name, attempted, entry.url, js_required)

        try:
            if js_required:
                html = fetch_html_js(entry.url)
            else:
                html = fetch_html(entry.url, session)
        # BUG FIX: `except (FetchError, Exception)` was redundant (FetchError is
        # already an Exception subclass) and just obscured that *every* exception
        # — including real bugs like AttributeError — was being swallowed here.
        # Kept as `Exception` since fetch_html_js can raise Playwright errors that
        # aren't FetchError, but this is now explicit rather than accidental.
        except Exception as exc:
            log.warning("Fetch failed (%s): %s", entry.url, exc)
            failed += 1
            time.sleep(RATE_LIMIT_SECONDS)
            continue

        try:
            body = extract_body(html, entry.url)
        except ExtractionError as exc:
            log.warning("Extraction failed (%s): %s", entry.url, exc)
            failed += 1
            time.sleep(RATE_LIMIT_SECONDS)
            continue

        article = Article(
            url=entry.url,
            title=entry.title,
            author=entry.author,
            published_at=entry.published_at,
            body_text=body,
            # BUG FIX: datetime.utcnow() is deprecated (Python 3.12+) and returns
            # a naive datetime, while dateutil-parsed RSS dates are usually
            # timezone-aware. Mixing the two raises TypeError on any later
            # comparison/subtraction (e.g. "time since publish"). Use an
            # explicit, aware UTC timestamp instead.
            scraped_at=datetime.now(timezone.utc),
            source=source.name,
        )
        save_article(article, output_path)
        succeeded += 1
        log.info("  Saved: %s", article.title[:80])
        time.sleep(RATE_LIMIT_SECONDS)

    return attempted, succeeded, failed


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> None:
    session = requests.Session()
    session.headers["User-Agent"] = USER_AGENT

    total_attempted = total_succeeded = total_failed = 0

    for source in SOURCES:
        a, s, f = scrape_source(source, session, OUTPUT_PATH)
        total_attempted += a
        total_succeeded += s
        total_failed += f

    log.info(
        "All done. Attempted: %d | Succeeded: %d | Failed: %d",
        total_attempted,
        total_succeeded,
        total_failed,
    )


if __name__ == "__main__":
    main()