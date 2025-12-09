swarm_build_all:
	docker build -t ai_service:latest ./ai_models
	docker build -t spring_app:latest ./demo
	docker build -t rss_worker:latest ./rss

swarm_build_ai:
	docker build -t ai_service:latest ./ai_models

swarm_build_rss:
	docker build -t rss_worker:latest ./rss

swarm_build_demo:
	docker build -t spring_app:latest ./demo

swarm_deploy:
	docker stack deploy -c stack.yml newsmonitor

swarm_check:
	docker stack services newsmonitor

swarm_down:
	docker stack rm newsmonitor