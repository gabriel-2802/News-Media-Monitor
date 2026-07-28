"""
Shared .env loading for all workers.

Every worker/module that reads configuration imports `require_env` (or the
typed variants) from here instead of calling `os.getenv` with an inline
default — this makes the root .env file the single source of truth and
fails loudly if a value is missing, instead of silently falling back to a
value baked into the code.
"""
from __future__ import annotations

import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parent.parent / ".env")


def require_env(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def require_int(name: str) -> int:
    return int(require_env(name))


def require_float(name: str) -> float:
    return float(require_env(name))
