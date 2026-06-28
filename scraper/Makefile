VENV   := venv
PYTHON := $(VENV)/bin/python
PIP    := $(VENV)/bin/pip

.PHONY: setup typecheck smoke scrape clean

setup:
	python3 -m venv $(VENV)
	$(PIP) install --quiet -r requirements.txt lxml_html_clean mypy types-requests types-python-dateutil
	$(PYTHON) -m playwright install chromium

typecheck:
	$(VENV)/bin/mypy --strict news_scraper.py

smoke:
	$(PYTHON) smoke_test.py

scrape:
	$(PYTHON) news_scraper.py

worker:
	$(PYTHON) worker.py

clean:
	rm -rf $(VENV) articles.jsonl smoke_test_results.json __pycache__
