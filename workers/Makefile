SCRAPER_VENV    := scraper/venv
CLASSIFIER_VENV := classifier/venv
CLUSTERER_VENV  := clusterer/venv

.PHONY: setup setup-scraper setup-classifier setup-clusterer scrape-worker classify-worker cluster-worker clean

setup: setup-scraper setup-classifier setup-clusterer

setup-scraper:
	python3 -m venv $(SCRAPER_VENV)
	$(SCRAPER_VENV)/bin/pip install --quiet --upgrade pip
	$(SCRAPER_VENV)/bin/pip install --quiet -r scraper/requirements.txt lxml_html_clean charset-normalizer
	$(SCRAPER_VENV)/bin/python -m playwright install chromium

setup-classifier:
	python3 -m venv $(CLASSIFIER_VENV)
	$(CLASSIFIER_VENV)/bin/pip install --quiet --upgrade pip
	$(CLASSIFIER_VENV)/bin/pip install --quiet -r classifier/requirements.txt

setup-clusterer:
	python3 -m venv $(CLUSTERER_VENV)
	$(CLUSTERER_VENV)/bin/pip install --quiet --upgrade pip
	$(CLUSTERER_VENV)/bin/pip install --quiet -r clusterer/requirements.txt

# Run from workers/ (this directory) so `scraper`/`classifier`/`clusterer`
# resolve as packages and all can import the shared provider_client.py.
scrape-worker:
	$(SCRAPER_VENV)/bin/python -m scraper.scraper_worker

classify-worker:
	$(CLASSIFIER_VENV)/bin/python -m classifier.classifier_worker

cluster-worker:
	$(CLUSTERER_VENV)/bin/python -m clusterer.clusterer_worker

clean:
	rm -rf $(SCRAPER_VENV) $(CLASSIFIER_VENV) $(CLUSTERER_VENV) __pycache__ scraper/__pycache__ classifier/__pycache__ clusterer/__pycache__
