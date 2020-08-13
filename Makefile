pull:
	docker-compose -f docker-compose/dev-monolith-env.yml pull

dev-monolith-up:
	docker-compose -f docker-compose/dev-monolith-env.yml up
dev-monolith-down:
	docker-compose -f docker-compose/dev-monolith-env.yml down

dev-monolith-start:
	docker-compose -f docker-compose/dev-monolith-env.yml start
dev-monolith-stop:
	docker-compose -f docker-compose/dev-monolith-env.yml stop
