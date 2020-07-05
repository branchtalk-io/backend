start-monolith-devel:
	docker-compose -f docker-compose/monolith-env.yml start

stop-monolith-devel:
	docker-compose -f docker-compose/monolith-env.yml stop
