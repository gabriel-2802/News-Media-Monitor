"""
log_config.py — shared colored console logging setup for all workers.

Call configure_logging() once per worker process, in place of
logging.basicConfig(). Keeps the exact same line format every worker
already used, just with level-based ANSI color.
"""
from __future__ import annotations

import logging

def _rgb(r: int, g: int, b: int) -> str:
    """24-bit truecolor escape — the basic 8/16-color ANSI codes have no
    pastel variants, so pastel requires an explicit RGB code."""
    return f"\033[38;2;{r};{g};{b}m"


_LEVEL_COLORS = {
    logging.DEBUG: _rgb(160, 210, 235),               # pastel blue
    logging.INFO: _rgb(174, 224, 178),                # pastel green
    logging.WARNING: _rgb(255, 213, 145),             # pastel amber
    logging.ERROR: _rgb(255, 165, 165),                # pastel coral/red
    logging.CRITICAL: "\033[1m" + _rgb(220, 165, 255), # bold pastel purple
}
_RESET = "\033[0m"


class ColorFormatter(logging.Formatter):
    """Colors the whole log line by level. Falls back to plain text when
    not writing to a real terminal (redirected to a file, piped through
    another program, etc.) so log files never fill up with escape codes."""

    def __init__(self, fmt: str, datefmt: str, use_color: bool) -> None:
        super().__init__(fmt=fmt, datefmt=datefmt)
        self._use_color = use_color

    def format(self, record: logging.LogRecord) -> str:
        message = super().format(record)
        color = _LEVEL_COLORS.get(record.levelno, "") if self._use_color else ""
        return f"{color}{message}{_RESET}" if color else message


def configure_logging(level: int = logging.INFO) -> None:
    handler = logging.StreamHandler()
    use_color = handler.stream.isatty()
    handler.setFormatter(ColorFormatter(
        fmt="%(asctime)s %(levelname)-8s %(message)s",
        datefmt="%H:%M:%S",
        use_color=use_color,
    ))

    root = logging.getLogger()
    root.setLevel(level)
    root.handlers.clear()
    root.addHandler(handler)
