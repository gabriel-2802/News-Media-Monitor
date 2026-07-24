"""Shared backoff retry for transient Provider outages.

Used by all three workers' message callbacks: a `ProviderError` means the
Provider service is unreachable, which is very likely transient (deploy,
restart, network blip), so it's worth retrying in place before giving up on
the whole worker.
"""
from __future__ import annotations

import logging
from typing import Callable, TypeVar

from provider_client import ProviderError

log = logging.getLogger(__name__)

T = TypeVar("T")

MAX_ATTEMPTS = 5
BASE_DELAY_SECONDS = 60.0


def call_with_retry(
    func: Callable[[], T],
    sleep: Callable[[float], None],
    max_attempts: int = MAX_ATTEMPTS,
    base_delay: float = BASE_DELAY_SECONDS,
) -> T:
    """Call `func()`, retrying on ProviderError with linear backoff
    (base_delay, 2*base_delay, 3*base_delay, ...).

    `sleep` should be `connection.sleep` (pika's BlockingConnection method),
    not `time.sleep` — it keeps the AMQP heartbeat alive during the wait.
    Re-raises ProviderError after the final attempt.
    """
    for attempt in range(1, max_attempts + 1):
        try:
            return func()
        except ProviderError as exc:
            if attempt == max_attempts:
                log.error(
                    "Provider still unreachable after %d attempts — giving up: %s",
                    max_attempts, exc,
                )
                raise
            delay = base_delay * attempt
            log.error(
                "Provider unreachable (attempt %d/%d) — retrying in %.0fs: %s",
                attempt, max_attempts, delay, exc,
            )
            sleep(delay)
    raise AssertionError("unreachable")  # max_attempts >= 1 guarantees a return or raise above
