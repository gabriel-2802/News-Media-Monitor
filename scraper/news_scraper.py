"""
News scraper — multi-source, RSS-first metadata, Playwright fallback for JS sites.

Architecture:
  BaseFetcher  → HttpFetcher | PlaywrightFetcher | SmartFetcher
  BaseExtractor → TrafilaturaExtractor
  BaseStorage  → JsonlStorage
  Scraper      → orchestrates one source end-to-end
"""
from __future__ import annotations

import json
import logging
import time
import urllib.robotparser
from abc import ABC, abstractmethod
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from typing import Optional
from urllib.parse import urljoin

import requests
import trafilatura
from bs4 import BeautifulSoup
from dateutil import parser as dateutil_parser
from dateutil.tz import tzoffset
from playwright.sync_api import sync_playwright

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

USER_AGENT = "NewsMonitorBot/0.1 (research scraper; contact: gabrielvalentine738@gmail.com)"
BROWSER_UA = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)
REQUEST_TIMEOUT: tuple[int, int] = (5, 10)
RATE_LIMIT_SECONDS = 2.0
MAX_RETRIES = 3
RETRY_BACKOFF = 3.0
JS_LOAD_WAIT_MS = 3000
JS_MIN_CHARS = 200
OUTPUT_PATH = "articles.jsonl"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)




# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class SourceConfig:
    name: str
    base_url: str
    rss_url: str
    rss_browser_ua: bool = False


SOURCES: list[SourceConfig] = [
    SourceConfig(name="bbc",         base_url="https://www.bbc.co.uk",         rss_url="https://feeds.bbci.co.uk/news/world/rss.xml"),
    SourceConfig(name="guardian",    base_url="https://www.theguardian.com",    rss_url="https://www.theguardian.com/world/rss"),
    SourceConfig(name="aljazeera",   base_url="https://www.aljazeera.com",      rss_url="https://www.aljazeera.com/xml/rss/all.xml"),
    SourceConfig(name="npr",         base_url="https://www.npr.org",            rss_url="https://feeds.npr.org/1001/rss.xml"),
    SourceConfig(name="dw",          base_url="https://www.dw.com",             rss_url="https://rss.dw.com/rdf/rss-en-all"),
    SourceConfig(name="france24",    base_url="https://www.france24.com",       rss_url="https://www.france24.com/en/rss"),
    SourceConfig(name="cbc",         base_url="https://www.cbc.ca",             rss_url="https://www.cbc.ca/cmlink/rss-world",        rss_browser_ua=True),
    SourceConfig(name="abc_au",      base_url="https://www.abc.net.au",         rss_url="https://www.abc.net.au/news/feed/51120/rss.xml"),
    SourceConfig(name="euronews",    base_url="https://www.euronews.com",       rss_url="https://www.euronews.com/rss"),
    SourceConfig(name="sky",         base_url="https://news.sky.com",           rss_url="https://feeds.skynews.com/feeds/rss/world.xml"),
    SourceConfig(name="independent", base_url="https://www.independent.co.uk",  rss_url="https://www.independent.co.uk/news/world/rss"),
    SourceConfig(name="thehill",     base_url="https://thehill.com",            rss_url="https://thehill.com/feed"),
    # Politico: Cloudflare blocks RSS endpoint — omitted
    SourceConfig(name="vox",         base_url="https://www.vox.com",            rss_url="https://www.vox.com/rss/index.xml"),
    SourceConfig(name="time",        base_url="https://time.com",               rss_url="https://time.com/feed"),
    SourceConfig(name="rt",          base_url="https://www.rt.com",             rss_url="https://www.rt.com/rss/news"),
]
# ---------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
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


@dataclass
class ScrapingStats:
    attempted: int = 0
    succeeded: int = 0
    failed: int = 0

    def __str__(self) -> str:
        return f"attempted={self.attempted} succeeded={self.succeeded} failed={self.failed}"


# ---------------------------------------------------------------------------
# Exceptions
# ---------------------------------------------------------------------------


class FetchError(Exception):
    pass


class ExtractionError(Exception):
    pass


# ---------------------------------------------------------------------------
# Fetchers
# ---------------------------------------------------------------------------


class BaseFetcher(ABC):
    @abstractmethod
    def fetch(self, url: str) -> str: ...


class HttpFetcher(BaseFetcher):
    """Static HTTP fetcher with retry logic."""

    def __init__(
        self,
        user_agent: str = USER_AGENT,
        timeout: tuple[int, int] = REQUEST_TIMEOUT,
        max_retries: int = MAX_RETRIES,
        retry_backoff: float = RETRY_BACKOFF,
    ) -> None:
        self._timeout = timeout
        self._max_retries = max_retries
        self._retry_backoff = retry_backoff
        self._session = requests.Session()
        self._session.headers["User-Agent"] = user_agent

    def with_user_agent(self, user_agent: str) -> HttpFetcher:
        clone = HttpFetcher(user_agent, self._timeout, self._max_retries, self._retry_backoff)
        return clone

    def fetch(self, url: str) -> str:
        last_exc: Exception = RuntimeError("no attempts made")
        for attempt in range(1, self._max_retries + 1):
            try:
                r = self._session.get(url, timeout=self._timeout)
                r.raise_for_status()
                return r.text
            except requests.HTTPError as exc:
                if exc.response is not None and exc.response.status_code in (404, 410):
                    raise FetchError(f"Permanent HTTP error for {url}: {exc}") from exc
                last_exc = exc
            except requests.RequestException as exc:
                last_exc = exc
            if attempt < self._max_retries:
                time.sleep(self._retry_backoff * attempt)
        raise FetchError(f"Failed {url} after {self._max_retries} attempts: {last_exc}") from last_exc


class PlaywrightFetcher(BaseFetcher):
    """Headless browser fetcher for JS-heavy pages."""

    def __init__(
        self,
        user_agent: str = BROWSER_UA,
        load_wait_ms: int = JS_LOAD_WAIT_MS,
        timeout_ms: float = REQUEST_TIMEOUT[1] * 1000,
    ) -> None:
        self._user_agent = user_agent
        self._load_wait_ms = load_wait_ms
        self._timeout_ms = timeout_ms

    def fetch(self, url: str) -> str:
        try:
            with sync_playwright() as pw:
                browser = pw.chromium.launch(headless=True, args=["--disable-http2"])
                page = browser.new_context(user_agent=self._user_agent).new_page()
                try:
                    page.goto(url, timeout=self._timeout_ms, wait_until="domcontentloaded")
                    page.wait_for_timeout(self._load_wait_ms)
                    return page.content()
                finally:
                    browser.close()
        except FetchError:
            raise
        except Exception as exc:
            raise FetchError(f"Browser fetch failed for {url}: {exc}") from exc


class SmartFetcher(BaseFetcher):
    """
    Fetch strategy (in order):
      1. Static fetch with bot UA  — fast, works for most sites
      2. Static fetch with browser UA — bypasses UA-based bot blocks (e.g. CBC, NPR)
      3. Playwright headless browser — for JS-rendered pages that need execution
    """

    def __init__(
        self,
        http: HttpFetcher,
        browser: PlaywrightFetcher,
        min_chars: int = JS_MIN_CHARS,
    ) -> None:
        self._http         = http
        self._http_browser_ua = http.with_user_agent(BROWSER_UA)
        self._browser      = browser
        self._min_chars    = min_chars

    def _body_length(self, html: str, url: str) -> int:
        raw = trafilatura.bare_extraction(html, url=url, include_comments=False, include_tables=False, as_dict=True)
        return len(str((raw or {}).get("text") or ""))  # type: ignore[union-attr]

    def fetch(self, url: str) -> str:
        # 1. Try bot UA static fetch
        try:
            html = self._http.fetch(url)
            if self._body_length(html, url) >= self._min_chars:
                log.info("SmartFetcher %s: static (bot UA)", url)
                return html
        except FetchError:
            pass

        # 2. Try browser UA static fetch (bypasses UA-based blocks)
        try:
            html = self._http_browser_ua.fetch(url)
            if self._body_length(html, url) >= self._min_chars:
                log.info("SmartFetcher %s: static (browser UA)", url)
                return html
        except FetchError:
            pass

        # 3. Fall back to Playwright for JS-rendered pages
        log.info("SmartFetcher %s: Playwright", url)
        return self._browser.fetch(url)


# ---------------------------------------------------------------------------
# Extractor
# ---------------------------------------------------------------------------


class BaseExtractor(ABC):
    @abstractmethod
    def extract(self, html: str, url: str) -> str: ...


class TrafilaturaExtractor(BaseExtractor):
    def extract(self, html: str, url: str) -> str:
        raw = trafilatura.bare_extraction(html, url=url, include_comments=False, include_tables=False, as_dict=True)
        if raw is None:
            raise ExtractionError(f"No content extracted for {url}")
        body = str(raw.get("text") or "")  # type: ignore[union-attr]
        if not body:
            raise ExtractionError(f"Empty body for {url}")
        return body


# ---------------------------------------------------------------------------
# Storage
# ---------------------------------------------------------------------------


class BaseStorage(ABC):
    @abstractmethod
    def save(self, article: Article) -> None: ...

    @abstractmethod
    def exists(self, url: str) -> bool: ...


class JsonlStorage(BaseStorage):
    def __init__(self, path: str = OUTPUT_PATH) -> None:
        self._path = path

    def save(self, article: Article) -> None:
        d = asdict(article)
        for key in ("published_at", "scraped_at"):
            if isinstance(d[key], datetime):
                d[key] = d[key].isoformat()
        with open(self._path, "a", encoding="utf-8") as fh:
            fh.write(json.dumps(d, ensure_ascii=False) + "\n")

    def exists(self, url: str) -> bool:
        return False  # JSONL has no index


# ---------------------------------------------------------------------------
# RSS parser (stateless, no ABC needed — one format)
# ---------------------------------------------------------------------------


def _text(tag: object) -> Optional[str]:
    if tag is None:
        return None
    s = getattr(tag, "string", None) or getattr(tag, "get_text", lambda: None)()
    return s.strip() if isinstance(s, str) and s.strip() else None


_TZINFOS: dict[str, tzoffset] = {
    "EDT": tzoffset("EDT", -4 * 3600),
    "EST": tzoffset("EST", -5 * 3600),
    "CDT": tzoffset("CDT", -5 * 3600),
    "CST": tzoffset("CST", -6 * 3600),
    "MDT": tzoffset("MDT", -6 * 3600),
    "MST": tzoffset("MST", -7 * 3600),
    "PDT": tzoffset("PDT", -7 * 3600),
    "PST": tzoffset("PST", -8 * 3600),
}


def _parse_date(date_str: Optional[str]) -> Optional[datetime]:
    if not date_str:
        return None
    try:
        return dateutil_parser.parse(date_str, tzinfos=_TZINFOS)
    except (ValueError, OverflowError):
        return None


def parse_rss(feed_html: str) -> list[RSSEntry]:
    soup = BeautifulSoup(feed_html, "lxml-xml")
    entries: list[RSSEntry] = []
    seen: set[str] = set()

    rss_items = soup.find_all("item")
    atom_entries = soup.find_all("entry")
    items = rss_items if rss_items else atom_entries
    is_atom = not rss_items and bool(atom_entries)

    for item in items:
        url: Optional[str] = None
        if is_atom:
            for link_tag in item.find_all("link"):
                if link_tag.get("href") and link_tag.get("rel", "alternate") in ("alternate", ""):
                    url = str(link_tag["href"])
                    break
            if not url:
                id_tag = item.find("id")
                url = _text(id_tag) if id_tag else None
        else:
            rss_link = item.find("link")
            if rss_link is not None:
                url = _text(rss_link) or str(rss_link.get("href", "") or "")
            if not url:
                guid = item.find("guid")
                url = _text(guid) if guid else None

        if not url:
            continue
        url = url.split("?")[0]
        if url in seen:
            continue
        seen.add(url)

        title = _text(item.find("title")) or ""
        if is_atom:
            author_tag = item.find("author")
            name_tag = author_tag.find("name") if author_tag else None
            author: Optional[str] = _text(name_tag)
            pub_str = _text(item.find("published")) or _text(item.find("updated"))
        else:
            author = _text(item.find("dc:creator")) or _text(item.find("author"))
            pub_str = _text(item.find("pubDate")) or _text(item.find("dc:date"))

        entries.append(RSSEntry(url=url, title=title, author=author, published_at=_parse_date(pub_str)))

    log.info("Parsed %d entries from %s feed", len(entries), "Atom" if is_atom else "RSS")
    return entries


# ---------------------------------------------------------------------------
# Robots checker
# ---------------------------------------------------------------------------


def _build_robots(base_url: str) -> urllib.robotparser.RobotFileParser:
    rp = urllib.robotparser.RobotFileParser()
    rp.set_url(urljoin(base_url, "/robots.txt"))
    try:
        rp.read()
    except Exception as exc:
        log.warning("Could not read robots.txt for %s: %s", base_url, exc)
    return rp


# ---------------------------------------------------------------------------
# Scraper
# ---------------------------------------------------------------------------


class Scraper:
    """
    Orchestrates scraping for any number of sources.

    Inject custom fetcher / extractor / storage to extend behaviour:
        scraper = Scraper(article_fetcher=MyFetcher(), storage=MyStorage())
    """

    def __init__(
        self,
        feed_fetcher: Optional[BaseFetcher] = None,
        article_fetcher: Optional[BaseFetcher] = None,
        extractor: Optional[BaseExtractor] = None,
        storage: Optional[BaseStorage] = None,
        rate_limit: float = RATE_LIMIT_SECONDS,
    ) -> None:
        http = HttpFetcher()
        self._feed_fetcher    = feed_fetcher    or http
        self._article_fetcher = article_fetcher or SmartFetcher(http, PlaywrightFetcher())
        self._extractor       = extractor       or TrafilaturaExtractor()
        self._storage         = storage         or JsonlStorage()
        self._rate_limit      = rate_limit

    def scrape_source(self, source: SourceConfig) -> ScrapingStats:
        log.info("=== Scraping: %s ===", source.name)
        stats = ScrapingStats()
        robots = _build_robots(source.base_url)

        feed_fetcher = (
            self._feed_fetcher.with_user_agent(BROWSER_UA)  # type: ignore[attr-defined]
            if source.rss_browser_ua and isinstance(self._feed_fetcher, HttpFetcher)
            else self._feed_fetcher
        )
        try:
            feed_html = feed_fetcher.fetch(source.rss_url)
        except FetchError as exc:
            log.error("Cannot fetch RSS for %s: %s", source.name, exc)
            return stats

        time.sleep(self._rate_limit)
        entries = parse_rss(feed_html)
        if not entries:
            log.warning("No entries for %s", source.name)
            return stats

        for entry in entries:
            if not robots.can_fetch(USER_AGENT, entry.url):
                log.info("robots.txt disallows %s", entry.url)
                continue
            if self._storage.exists(entry.url):
                continue

            stats.attempted += 1
            log.info("[%s] Fetching %s", source.name, entry.url)

            try:
                html = self._article_fetcher.fetch(entry.url)
                body = self._extractor.extract(html, entry.url)
            except (FetchError, ExtractionError, Exception) as exc:
                log.warning("Failed (%s): %s", entry.url, exc)
                stats.failed += 1
                time.sleep(self._rate_limit)
                continue

            article = Article(
                url=entry.url,
                title=entry.title,
                author=entry.author,
                published_at=entry.published_at,
                body_text=body,
                scraped_at=datetime.now(timezone.utc),
                source=source.name,
            )
            self._storage.save(article)
            stats.succeeded += 1
            log.info("  Saved: %s", article.title[:80])
            time.sleep(self._rate_limit)

        return stats

    def scrape_all(self, sources: list[SourceConfig]) -> ScrapingStats:
        total = ScrapingStats()
        for source in sources:
            s = self.scrape_source(source)
            total.attempted += s.attempted
            total.succeeded += s.succeeded
            total.failed    += s.failed
        log.info("Done. %s", total)
        return total


# ---------------------------------------------------------------------------
# Backward-compatible module-level helpers (used by smoke_test.py)
# ---------------------------------------------------------------------------


def fetch_html(url: str, session: Optional[requests.Session] = None) -> str:
    fetcher = HttpFetcher()
    if session is not None:
        fetcher._session = session  # reuse caller's session / headers
    return fetcher.fetch(url)


def requires_js(url: str, session: Optional[requests.Session] = None) -> bool:
    http = HttpFetcher()
    if session is not None:
        http._session = session
    smart = SmartFetcher(http, PlaywrightFetcher())
    # Probe: attempt static fetches; returns True only if both fail body threshold
    try:
        html = smart._http.fetch(url)
        if smart._body_length(html, url) >= smart._min_chars:
            return False
    except FetchError:
        pass
    try:
        html = smart._http_browser_ua.fetch(url)
        if smart._body_length(html, url) >= smart._min_chars:
            return False
    except FetchError:
        pass
    return True


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------


def main() -> None:
    Scraper().scrape_all(SOURCES)


if __name__ == "__main__":
    main()
