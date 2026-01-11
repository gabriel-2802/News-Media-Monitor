.PHONY: db build_all build_ai build_rss build_demo deploy check down

db:
	@id=$$(docker ps -q --filter "name=newsmonitor_db"); \
	if [ -z "$$id" ]; then \
		echo "DB container not running" 1>&2; \
		exit 1; \
	fi; \
	docker exec -it $$id psql -U admin -d news_monitor_db

build_all:
	docker build -t ai_service:latest ./ai_models
	docker build -t spring_app:latest ./demo
	docker build -t rss_worker:latest ./rss

build_ai:
	docker build -t ai_service:latest ./ai_models

build_rss:
	docker build -t rss_worker:latest ./rss

build_main:
	docker build -t spring_app:latest ./demo

deploy:
	docker stack deploy -c stack.yml newsmonitor

check_main:
	docker service logs -f --timestamps newsmonitor_spring_app \
	| sed 's/^/\x1b[32m[SPRING]\x1b[0m /'

check_rss:
	docker service logs -f --timestamps newsmonitor_rss_worker \
	| sed 's/^/\x1b[36m[RSS]\x1b[0m /'

check_ai:
	docker service logs -f --timestamps newsmonitor_ai_service \
	| sed 's/^/\x1b[35m[AI]\x1b[0m /'


check:
	docker stack services newsmonitor

down:
	docker stack rm newsmonitor
