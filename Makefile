run:
	docker compose up -d

psql:
	docker exec -it news_monitor_db psql -U admin -d news_monitor_db

stop:
	docker compose down