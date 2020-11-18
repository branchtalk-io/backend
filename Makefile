.EXPORT_ALL_VARIABLES:

NETWORK_PREFIX=$(shell basename $(PWD))
CONFIG_DIR=docker-compose
DEV_CONFIGS=--file ${CONFIG_DIR}/monolith-deps.yml --file ${CONFIG_DIR}/monolith-setup.yml

test:
	echo $(NETWORK_NAME)

pull:
	docker-compose --project-directory . ${DEV_CONFIGS} pull

dev-bg:
	docker-compose --project-directory . ${DEV_CONFIGS} up --detach --quiet-pull
dev-up:
	docker-compose --project-directory . ${DEV_CONFIGS} up
dev-stop:
	docker-compose --project-directory . ${DEV_CONFIGS} stop
dev-down:
	docker-compose --project-directory . ${DEV_CONFIGS} down --remove-orphans || \
	(docker container rm $(NETWORK_PREFIX)_kafka_1 $(NETWORK_PREFIX)_postgres_1 -f && \
	 docker network disconnect $(NETWORK_PREFIX)_branchtalk-monolith $(NETWORK_PREFIX)_kafka_1 -f && \
   docker network disconnect $(NETWORK_PREFIX)_branchtalk-monolith $(NETWORK_PREFIX)_postgres_1 -f && \
   docker network rm $(NETWORK_PREFIX)_branchtalk-monolith)
dev-ps:
	docker-compose --project-directory . ${DEV_CONFIGS} ps
dev-logs:
	docker-compose --project-directory . ${DEV_CONFIGS} logs ${LOGS}
dev-clean-volumes:
	docker volume rm $(NETWORK_PREFIX)_postgres_data -f
	docker volume rm $(NETWORK_PREFIX)_kafka_data -f
	docker volume rm $(NETWORK_PREFIX)_zookeeper_data -f
