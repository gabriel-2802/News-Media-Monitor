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
    from news_scraper import Article

log = logging.getLogger(__name__)

# Endpoints
_SOURCE_PATH = "/api/news-sources/{name}"
_SOURCE_FAILURE_PATH = "/api/news-sources/{name}/failure"
_SOURCE_RESET_PATH = "/api/news-sources/{name}/reset"
_ARTICLES_PATH = "/api/articles"
_ARTICLE_EXISTS_PATH = "/api/articles/exists"

_NOT_FOUND_OR_REJECTED = HTTPStatus.BAD_REQUEST


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