.DEFAULT_GOAL := help

COMPOSE           := docker compose
NEO4J_PASSWORD    ?= secretsecret
RABBITMQ_USER     ?= admin
RABBITMQ_PASSWORD ?= secret
RABBITMQ_VHOST    ?= news_monitor

.PHONY: help
help: ## Show available targets
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n\nTargets:\n"} \
	  /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2 }' \
	  $(MAKEFILE_LIST)

# ── Bootstrap ──────────────────────────────────────────────────────────────────

.PHONY: env
env: ## Copy .env.example → .env (skips if .env already exists)
	@if [ -f .env ]; then \
		echo ".env already exists — skipping."; \
	else \
		cp .env.example .env && echo "Created .env from .env.example."; \
	fi

.PHONY: dev
dev: env up migrate rabbitmq-setup ## Bootstrap the dev environment: create .env, start services, run migrations and setup
	@echo ""
	@echo "✓ Ready!"
	@echo "Neo4j    → http://localhost:7474  (neo4j / $(NEO4J_PASSWORD))"
	@echo "RabbitMQ → http://localhost:15672 ($(RABBITMQ_USER) / $(RABBITMQ_PASSWORD))"

# ── Infrastructure ─────────────────────────────────────────────────────────────

.PHONY: up
up: ## Start all services in the background
	$(COMPOSE) up -d

.PHONY: down
down: ## Stop all services
	$(COMPOSE) down

.PHONY: restart
restart: down up ## Restart all services

.PHONY: pull
pull: ## Pull the latest Docker images
	$(COMPOSE) pull

.PHONY: ps
ps: ## Show running containers and their health status
	$(COMPOSE) ps

.PHONY: status
status: ps

# ── Logs ───────────────────────────────────────────────────────────────────────

.PHONY: logs
logs: ## Stream logs for all services (Ctrl-C to stop)
	$(COMPOSE) logs -f

.PHONY: logs-neo4j
logs-neo4j: ## Stream Neo4j logs
	$(COMPOSE) logs -f neo4j

.PHONY: logs-rabbitmq
logs-rabbitmq: ## Stream RabbitMQ logs
	$(COMPOSE) logs -f rabbitmq

# ── Cleanup ────────────────────────────────────────────────────────────────────

.PHONY: clean
clean: ## Stop services and DELETE all volumes — DESTRUCTIVE
	@echo "⚠️  WARNING: this removes all persisted Neo4j and RabbitMQ data."
	@read -r -p "Continue? [y/N] " ans && [ "$$ans" = "y" ] || exit 1
	$(COMPOSE) down -v --remove-orphans

.PHONY: clean-images
clean-images: clean ## Remove volumes and prune dangling images
	docker image prune -f

# ── Neo4j ──────────────────────────────────────────────────────────────────────

.PHONY: neo4j-shell
neo4j-shell: ## Open an interactive Cypher Shell session
	$(COMPOSE) exec neo4j cypher-shell -u neo4j -p $(NEO4J_PASSWORD)

.PHONY: neo4j-url
neo4j-url: ## Print the Neo4j Browser URL and credentials
	@echo "Neo4j Browser → http://localhost:7474"
	@echo "  username : neo4j"
	@echo "  password : $(NEO4J_PASSWORD)"

.PHONY: neo4j-wait
neo4j-wait: ## Block until Neo4j accepts connections
	@printf "Waiting for Neo4j"
	@until $(COMPOSE) exec -T neo4j cypher-shell -u neo4j -p $(NEO4J_PASSWORD) "RETURN 1" > /dev/null 2>&1; do \
		printf "."; sleep 3; \
	done
	@echo " ready."

# ── Migrations ─────────────────────────────────────────────────────────────────

.PHONY: migrate
migrate: neo4j-wait ## Apply all pending Neo4j migrations
	@echo "Running migrations..."
	$(COMPOSE) exec -T -e NEO4J_PASSWORD=$(NEO4J_PASSWORD) neo4j bash /migrate.sh

.PHONY: migrate-info
migrate-info: ## List applied migrations stored in Neo4j
	$(COMPOSE) exec neo4j cypher-shell -u neo4j -p $(NEO4J_PASSWORD) \
	  "MATCH (m:\`__Migration\`) RETURN m.version, m.filename, m.applied_at ORDER BY m.version;"

.PHONY: migrate-clean
migrate-clean: ## Delete all migration tracking nodes — dev only, DESTRUCTIVE
	@echo "WARNING: this removes all migration history from Neo4j (does not undo schema changes)."
	@read -r -p "Continue? [y/N] " ans && [ "$$ans" = "y" ] || exit 1
	$(COMPOSE) exec neo4j cypher-shell -u neo4j -p $(NEO4J_PASSWORD) \
	  "MATCH (m:\`__Migration\`) DELETE m;"

.PHONY: _neo4j-purge-exec
_neo4j-purge-exec:
	$(COMPOSE) exec neo4j cypher-shell -u neo4j -p $(NEO4J_PASSWORD) \
	  "MATCH (n) DETACH DELETE n;"

.PHONY: neo4j-purge
neo4j-purge: ## Delete ALL data in Neo4j — nodes, relationships, migration history — DESTRUCTIVE
	@echo "⚠️  WARNING: this deletes ALL data in Neo4j (articles, sources, stories, topics, migration history)."
	@read -r -p "Continue? [y/N] " ans && [ "$$ans" = "y" ] || exit 1
	@$(MAKE) _neo4j-purge-exec
	@echo "✓ Neo4j purged. Run 'make migrate' to reapply schema constraints if needed."

# ── RabbitMQ ───────────────────────────────────────────────────────────────────

.PHONY: rabbitmq-setup
rabbitmq-setup: ## Re-run topology setup (idempotent — safe to repeat)
	@echo "Setting up RabbitMQ topology..."
	$(COMPOSE) exec -T rabbitmq bash /setup.sh

.PHONY: rabbitmq-url
rabbitmq-url: ## Print the RabbitMQ Management UI URL and credentials
	@echo "RabbitMQ Management → http://localhost:15672"
	@echo "  username : $(RABBITMQ_USER)"
	@echo "  password : $(RABBITMQ_PASSWORD)"
	@echo "  vhost    : $(RABBITMQ_VHOST)"

.PHONY: rabbitmq-shell
rabbitmq-shell: ## Open a bash shell inside the RabbitMQ container
	$(COMPOSE) exec rabbitmq bash

.PHONY: rabbitmq-list-queues
rabbitmq-list-queues: ## List queues on the news_monitor vhost
	$(COMPOSE) exec rabbitmq rabbitmqctl list_queues \
		-p $(RABBITMQ_VHOST) name messages consumers state

.PHONY: rabbitmq-list-exchanges
rabbitmq-list-exchanges: ## List exchanges on the news_monitor vhost
	$(COMPOSE) exec rabbitmq rabbitmqctl list_exchanges \
		-p $(RABBITMQ_VHOST) name type durable auto_delete

.PHONY: rabbitmq-list-bindings
rabbitmq-list-bindings: ## List bindings on the news_monitor vhost
	$(COMPOSE) exec rabbitmq rabbitmqctl list_bindings \
		-p $(RABBITMQ_VHOST)

.PHONY: rabbitmq-purge-scrape-jobs
rabbitmq-purge-scrape-jobs: ## Purge the scrape.jobs queue — DESTRUCTIVE
	@echo "⚠️  WARNING: all queued messages in scrape.jobs will be discarded."
	@read -r -p "Continue? [y/N] " ans && [ "$$ans" = "y" ] || exit 1
	$(COMPOSE) exec rabbitmq rabbitmqctl purge_queue scrape.jobs -p $(RABBITMQ_VHOST)

# ── Qdrant ─────────────────────────────────────────────────────────────────────
# Qdrant is not a docker-compose service (it's Qdrant Cloud) — QDRANT_URL and
# QDRANT_API_KEY come from .env, not from Makefile variables.

.PHONY: _qdrant-purge-exec
_qdrant-purge-exec:
	@set -a && . .env && set +a && \
	collection=$${CENTROID_COLLECTION:-story_centroids}; \
	status=$$(curl -sS -o /dev/null -w "%{http_code}" -X DELETE \
	  "$$QDRANT_URL/collections/$$collection" -H "api-key: $$QDRANT_API_KEY"); \
	if [ "$$status" = "200" ]; then \
	  echo "✓ Qdrant collection '$$collection' purged."; \
	else \
	  echo "✗ Qdrant purge failed (HTTP $$status)"; exit 1; \
	fi

.PHONY: qdrant-purge
qdrant-purge: ## Delete the story_centroids Qdrant collection (recreated automatically on next cluster-worker start) — DESTRUCTIVE
	@echo "⚠️  WARNING: this deletes all story centroid vectors in Qdrant."
	@read -r -p "Continue? [y/N] " ans && [ "$$ans" = "y" ] || exit 1
	@$(MAKE) _qdrant-purge-exec

# ── Combined cleanup ───────────────────────────────────────────────────────────

.PHONY: purge-dbs
purge-dbs: ## Wipe ALL data in both Neo4j and Qdrant — DESTRUCTIVE
	@echo "⚠️  WARNING: this deletes ALL data in Neo4j AND Qdrant. This cannot be undone."
	@read -r -p "Continue? [y/N] " ans && [ "$$ans" = "y" ] || exit 1
	@$(MAKE) _neo4j-purge-exec
	@$(MAKE) _qdrant-purge-exec
	@echo "✓ Both databases purged. Run 'make migrate' to reapply Neo4j schema constraints if needed."