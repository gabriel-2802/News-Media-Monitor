VENV   := venv
PYTHON := $(VENV)/bin/python
PIP    := $(VENV)/bin/pip

.PHONY: setup typecheck smoke scrape clean

setup:
	python3 -m venv $(VENV)
	$(PIP) install --quiet -r requirements.txt lxml_html_clean mypy types-requests types-python-dateutil
	$(PYTHON) -m playwright install chromium

typecheck:
	@test -x $(PYTHON) || python3 -m venv $(VENV)
	@if ! $(PIP) show mypy >/dev/null 2>&1; then \
		if ! $(PIP) install --quiet mypy; then \
			python3 -m venv --clear $(VENV); \
			$(PIP) install --quiet -r requirements.txt lxml_html_clean mypy types-requests types-python-dateutil; \
		fi; \
	fi
	$(PYTHON) -m mypy --strict news_scraper.py

scrape:
	$(PYTHON) news_scraper.py

worker:
	$(PYTHON) worker.py

clean:
	rm -rf $(VENV) articles.jsonl smoke_test_results.json __pycache__
