# News Scraper — Strategy & Architecture (Proof of Concept)

## Goal

Build a single Python 3 script, fully typed, that performs a **full scrape**
of one news source end-to-end: discover article URLs on that source, fetch
each article page, and extract structured data (title, author, publish date,
full body text). This is the proof of concept; ongoing monitoring,
deduplication, and alerting are explicitly out of scope for this phase and
come later.

Worked example target: a generic major news site with a standard structure —
a homepage/section page listing article links, and individual article pages
with a headline, byline, timestamp, and body copy (e.g. Reuters/BBC-style
markup). The design should not hardcode logic that only works for one site;
isolate site-specific knowledge into one layer (see Architecture).

---

## Scope of the POC

**In scope:**
- One news source
- Crawl a section/homepage to collect article URLs
- Fetch each article
- Extract: title, author(s), published date, body text, URL
- Save results to disk (JSON Lines — one JSON object per line, easy to append/inspect)
- Basic politeness: rate limiting, custom User-Agent, timeout/retry
- Logging of progress and failures
- Generic extraction (must work for an unknown site)

**Out of scope (later phases):**
- Scheduling / running continuously
- Deduplication across runs
- Multi-source support
- Alerting (email/Slack/etc.)
- JavaScript-rendered pages (headless browser) — only needed if the target
  site requires it; first POC assumes static/server-rendered HTML

---

## Architecture

Four layers, kept separate so any one of them can be swapped or fixed
without touching the others. This separation matters most for the
**extraction** layer — that's the part that breaks when a site redesigns,
so it should never be tangled into the fetching or crawling logic.

```
┌─────────────────┐
│ 1. Link Discovery │  → list of article URLs
└────────┬─────────┘
         ↓
┌─────────────────┐
│ 2. Fetcher        │  → raw HTML (with retry/throttle/headers)
└────────┬─────────┘
         ↓
┌─────────────────┐
│ 3. Extractor      │  → structured Article object
└────────┬─────────┘
         ↓
┌─────────────────┐
│ 4. Storage        │  → JSONL file on disk
└──────────────────┘
```

### 1. Link Discovery

Fetch the homepage or a section page, parse the HTML, collect `<a>` tags
that match the site's article URL pattern (e.g. contain `/article/`,
`/news/`, a date pattern, etc.). Filter out nav links, ads, external links.

- Use `BeautifulSoup` for this — it's just link extraction, no need for a
  heavier tool yet.
- Keep the URL-matching pattern as a config value (regex or path prefix),
  not buried in logic, since this is the first thing that breaks per-site.

### 2. Fetcher

A single function responsible for getting raw HTML for a URL, with:
- A realistic `User-Agent` header (some sites 403 default Python UAs)
- A timeout (e.g. 10s)
- Retry with backoff on transient failures (use `tenacity`, or hand-roll a
  simple retry loop for the POC)
- Rate limiting between requests (`time.sleep`, e.g. 1–2 sec minimum)

This layer knows nothing about news — it just returns HTML or raises.

### 3. Extractor

This is the layer most likely to need rework, so isolate it cleanly.


- **Generic extraction** via `trafilatura` — point it at the raw HTML, it
  strips boilerplate (nav, ads, related-articles widgets) and returns title,
  text, author, and date in one call. Best default for a source-agnostic
  POC since it requires no site-specific selectors.

Recommendation for "first POC, one source": start with `trafilatura` for
speed, since it works across most standard news layouts out of the box. If
results are inaccurate or incomplete for the specific site picked, fall
back to custom selectors as a second pass.

Either way, model the output as a single typed structure (see Data Model)
so the storage layer never needs to know which extraction method produced
it.

### 4. Storage

Append each extracted article as a single line of JSON to a `.jsonl` file.
JSONL is the right format here over a single JSON array because:
- Safe to append incrementally without rewriting the whole file
- Easy to resume / inspect / `grep` 
- Trivial to load into pandas later if needed

A database (SQLite) is unnecessary for this POC's scope — dedup/state
tracking is a later-phase concern.

---

## Data Model

Use a typed structure (`dataclass` or `TypedDict`) for every extracted
article so the script is internally consistent and type-checkable:

| Field | Type | Notes |
|---|---|---|
| `url` | `str` | canonical article URL |
| `title` | `str` | |
| `author` | `str \| None` | not always present |
| `published_at` | `datetime \| None` | parse if possible, else `None` |
| `body_text` | `str` | full extracted article text |
| `scraped_at` | `datetime` | when *this script* fetched it, always set |
| `source` | `str` | domain/name of the news source |

Use `Optional[...]` explicitly rather than leaving fields untyped — several
of these will legitimately be missing depending on the article.

---

## Typing Approach

Since this needs to be "fully typed":
- Type every function signature (params and return types), no bare `Any`
  unless interfacing with an untyped third-party return value
- Use `dataclass` for the `Article` model
- Run `mypy --strict` against the script as a sanity check before calling
  the POC done
- Wrap fetch/extract failures in explicit exception types rather than
  letting bare `Exception` propagate, so failure modes are typed too

---

## Error Handling & Politeness (don't skip for the POC)

Even at proof-of-concept stage, build these in from the start — retrofitting
politeness after getting IP-blocked is a worse position to debug from:

- Check `robots.txt` for the target source before crawling; respect
  disallowed paths
- Set a descriptive `User-Agent` (can include contact info — good practice,
  not just to avoid blocks)
- Catch and log per-article failures; one bad page should not kill the run
- Sleep between requests; no concurrency in the POC (concurrency is a
  later-phase optimization once correctness is proven)

---

## Suggested Script Structure

A single file is fine for the POC. Rough shape (function names indicative,
not prescriptive):

```
news_scraper.py
├── Article (dataclass)
├── fetch_html(url) -> str
├── discover_article_links(homepage_html, base_url) -> list[str]
├── extract_article(html, url, source) -> Article | None
├── save_article(article, output_path) -> None
└── main() -> None   # wires the above together, handles top-level logging
```

`main()` should:
1. Fetch the homepage/section page
2. Discover article links
3. Loop over links: fetch → extract → save, with rate limiting and
   per-item error handling
4. Log a summary (N attempted, N succeeded, N failed) at the end

---

## Dependencies

USE a venvironment (venv/conda) for this project. 

| Library | Purpose |
|---|---|
| `requests` | HTTP fetching |
| `beautifulsoup4` | link discovery (and fallback extraction) |
| `trafilatura` | full-article content extraction |
| `python-dateutil` | parsing inconsistent date formats from articles |
| `mypy` (dev only) | static type checking |

Install:
```
pip install requests beautifulsoup4 trafilatura python-dateutil
```

---

## Validation Plan for the POC

Before calling this "done":
1. Run against the chosen source, confirm it pulls **at least 10–20**
   articles cleanly
2. Spot-check 3–5 extracted articles manually against the live page —
   confirm title/author/date/body are correct, not boilerplate
3. Confirm `mypy --strict` passes
4. Confirm a deliberately broken URL (404) doesn't crash the whole run
5. Confirm the JSONL output loads correctly back into Python (`json.loads`
   per line)

---

## Path to Next Phase (monitoring/alerts)

Once this POC works for one source, the upgrade path already discussed is:
add a "seen" store (SQLite) to dedupe across runs, wrap the script in a
scheduler (cron / GitHub Actions), and add an alert step (email/webhook)
when new articles match keyword criteria. None of that requires reworking
the four layers above — it wraps around them.