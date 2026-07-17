"""
HTTP client for the Provider service.

Neo4j is not accessed directly by the scraper: the Provider service is the
only path to source and article data.
"""
from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import datetime, timezone
from http import HTTPStatus
from typing import TYPE_CHECKING, Any, Optional

import requests

if TYPE_CHECKING:
    from workers.scraper.news_scraper import Article

log = logging.getLogger(__name__)

# Endpoints
_SOURCE_PATH = "/api/news-sources/{name}"
_SOURCE_FAILURE_PATH = "/api/news-sources/{name}/failure"
_SOURCE_RESET_PATH = "/api/news-sources/{name}/reset"
_ARTICLES_PATH = "/api/articles"
_ARTICLE_EXISTS_PATH = "/api/articles/exists"
_ARTICLE_BY_URL_PATH = "/api/articles/by-url"
_ARTICLE_TOPIC_PATH = "/api/articles/topic"
_ARTICLES_BY_STORY_PATH = "/api/articles/story/{storyId}"
_STORIES_PATH = "/api/stories"
_STORIES_RECENT_PATH = "/api/stories/recent"
_STORY_ATTACH_PATH = "/api/stories/{storyId}/attach"

_NOT_FOUND_OR_REJECTED = HTTPStatus.BAD_REQUEST


def _parse_instant(value: str) -> datetime:
    """Parse a java.time.Instant serialized by Jackson (ISO-8601 UTC, 'Z'
    suffix) — Python's fromisoformat doesn't accept 'Z' before 3.11."""
    if value.endswith("Z"):
        value = value[:-1] + "+00:00"
    return datetime.fromisoformat(value)


class ProviderError(Exception):
    """The Provider service is unreachable or returned an unexpected error."""


@dataclass(frozen=True)
class SourceInfo:
    name: str
    base_url: str
    rss_url: str
    disabled: bool
    failure_count: int

    @classmethod
    def from_dict(cls, row: dict[str, Any]) -> "SourceInfo":
        return cls(
            name=row["name"],
            base_url=row["baseUrl"],
            rss_url=row["rssUrl"],
            disabled=row["disabled"],
            failure_count=row["failureCount"],
        )
    
@dataclass(frozen=True)
class ArticleInfo:
    author: Optional[str]
    title: str
    url: str
    body_text: str
    published_at: datetime
    source: str

    @classmethod
    def from_dict(cls, row: dict[str, Any]) -> "ArticleInfo":
        return cls(
            author=row["author"],
            title=row["title"],
            url=row["url"],
            body_text=row["bodyText"],
            published_at=datetime.fromisoformat(row["publishedAt"]).replace(tzinfo=timezone.utc),
            source=row["source"],
        )


@dataclass(frozen=True)
class StoryInfo:
    id: str
    title: str
    created_at: datetime
    last_updated: datetime
    article_count: int
    source_count: int
    trending_score: float

    @classmethod
    def from_dict(cls, row: dict[str, Any]) -> "StoryInfo":
        return cls(
            id=row["id"],
            title=row["title"],
            created_at=_parse_instant(row["createdAt"]),
            last_updated=_parse_instant(row["lastUpdated"]),
            article_count=row["articleCount"],
            source_count=row["sourceCount"],
            trending_score=row["trendingScore"],
        )


class ProviderClient:
    def __init__(self, base_url: str, timeout: float = 10.0) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._session = requests.Session()

    def get_source(self, name: str) -> Optional[SourceInfo]:
        """Look up a registered source by name. Returns None if no such source exists."""
        resp = self._request("GET", _SOURCE_PATH.format(name=name))
        if resp.status_code == _NOT_FOUND_OR_REJECTED:
            return None
        return SourceInfo.from_dict(self._json_or_raise(resp))

    def record_failure(self, name: str) -> bool:
        """Increment the source's consecutive-failure counter. Returns True if now disabled."""
        resp = self._request("PATCH", _SOURCE_FAILURE_PATH.format(name=name))
        return bool(self._json_or_raise(resp)["disabled"])

    def reset_failures(self, name: str) -> None:
        """Clear the failure counter and re-enable the source."""
        resp = self._request("PATCH", _SOURCE_RESET_PATH.format(name=name))
        self._json_or_raise(resp)

    def article_exists(self, url: str) -> bool:
        resp = self._request("GET", _ARTICLE_EXISTS_PATH, params={"url": url})
        return bool(self._json_or_raise(resp))

    def save_article(self, article: "Article") -> bool:
        """POST a scraped article.

        Returns True if saved, False if the provider rejected it (duplicate
        URL, unknown source, URL/source mismatch -- all reported as HTTP 400).
        """
        resp = self._request("POST", _ARTICLES_PATH, json=self._article_payload(article))
        if resp.status_code == HTTPStatus.CREATED:
            return True
        if resp.status_code == _NOT_FOUND_OR_REJECTED:
            log.warning("Article rejected by provider (%s): %s", article.url, resp.text)
            return False
        raise ProviderError(
            f"POST {_ARTICLES_PATH}: unexpected status {resp.status_code}: {resp.text}"
        )
    
    def get_article(self, url: str) -> Optional[ArticleInfo]:
        """Look up an article by its canonical URL. Returns None if no such article exists."""
        resp = self._request("GET", _ARTICLE_BY_URL_PATH, params={"url": url})
        if resp.status_code == _NOT_FOUND_OR_REJECTED:
            return None
        return ArticleInfo.from_dict(self._json_or_raise(resp))

    def set_article_topic(self, url: str, topic: str) -> ArticleInfo:
        """Tag the article at the given URL with a topic, replacing any topic it already had."""
        resp = self._request("PATCH", _ARTICLE_TOPIC_PATH, json={"url": url, "topic": topic})
        return ArticleInfo.from_dict(self._json_or_raise(resp))

    def get_articles_by_story(self, story_id: str, page: int = 0, count: int = 20) -> list[ArticleInfo]:
        """Articles belonging to a story, most-recently-published first.

        There's no vector DB yet, so the clustering worker uses this to
        pull a story's representative article(s) and re-embed them on the
        fly instead of comparing against a stored centroid.
        """
        resp = self._request(
            "GET", _ARTICLES_BY_STORY_PATH.format(storyId=story_id), params={"page": page, "count": count}
        )
        return [ArticleInfo.from_dict(row) for row in self._json_or_raise(resp)]

    def create_story(self, title: str) -> StoryInfo:
        """POST a new story cluster, seeded with a title (usually the first article's)."""
        resp = self._request("POST", _STORIES_PATH, params={"title": title})
        if resp.status_code != HTTPStatus.CREATED:
            raise ProviderError(f"POST {_STORIES_PATH}: unexpected status {resp.status_code}: {resp.text}")
        return StoryInfo.from_dict(resp.json())

    def attach_article_to_story(self, story_id: str, article_url: str) -> Optional[StoryInfo]:
        """Attach an article to a story. Returns None if the provider rejects it
        (story or article doesn't exist -- reported as HTTP 400)."""
        resp = self._request(
            "PATCH", _STORY_ATTACH_PATH.format(storyId=story_id), json={"articleUrl": article_url}
        )
        if resp.status_code == _NOT_FOUND_OR_REJECTED:
            return None
        return StoryInfo.from_dict(self._json_or_raise(resp))

    def get_recent_stories(self, days: int = 7) -> list[StoryInfo]:
        """Stories with activity in the last `days` days -- the clustering
        worker's candidate pool for attach-vs-create decisions."""
        resp = self._request("GET", _STORIES_RECENT_PATH, params={"days": days})
        return [StoryInfo.from_dict(row) for row in self._json_or_raise(resp)]

    def get_stories(self, page: int = 0, count: int = 20) -> list[StoryInfo]:
        resp = self._request("GET", _STORIES_PATH, params={"page": page, "count": count})
        return [StoryInfo.from_dict(row) for row in self._json_or_raise(resp)]

    @staticmethod
    def _article_payload(article: "Article") -> dict[str, Any]:
        published_at: datetime = article.published_at or article.scraped_at
        return {
            "author": article.author,
            "title": article.title,
            "url": article.url,
            "bodyText": article.body_text,
            "publishedAt": published_at.astimezone(timezone.utc).replace(tzinfo=None).isoformat(),
            "sourceName": article.source,
        }

    def _request(self, method: str, path: str, **kwargs: Any) -> requests.Response:
        """Issue a request, translating transport-level failures into ProviderError.

        Callers are responsible for interpreting the resulting status code,
        since "expected" non-200 statuses (e.g. 400 meaning "not found") vary
        by endpoint.
        """
        try:
            return self._session.request(
                method, f"{self._base_url}{path}", timeout=self._timeout, **kwargs
            )
        except requests.RequestException as exc:
            raise ProviderError(f"{method} {path} failed: {exc}") from exc

    @staticmethod
    def _json_or_raise(resp: requests.Response) -> Any:
        """Raise ProviderError for any non-2xx status, otherwise return the parsed body."""
        if not resp.ok:
            raise ProviderError(
                f"{resp.request.method} {resp.request.path_url}: "
                f"unexpected status {resp.status_code}: {resp.text}"
            )
        return resp.json()