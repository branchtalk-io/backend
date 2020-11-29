.EXPORT_ALL_VARIABLES:

NETWORK_PREFIX=$(shell basename $(PWD))
CONFIG_DIR=docker-compose
DEV_CONFIGS=--file ${CONFIG_DIR}/monolith-deps.yml --file ${CONFIG_DIR}/monolith-setup.yml
LOCAL_CONFIGS=${DEV_CONFIGS} --file ${CONFIG_DIR}/monolith-app.yml

# fetch dependencies
pull:
	docker-compose --project-directory . ${DEV_CONFIGS} pull

# dev environment to use with sbt
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
	docker-compose --project-directory . ${DEV_CONFIGS} logs -f ${LOGS}

# whole monolithic app setup for e.g. local frontend development
local-bg:
	(docker-compose --project-directory . ${LOCAL_CONFIGS} up --detach --quiet-pull) || (echo "publish application with sbt application/docker:publishLocal!")
local-up:
	(docker-compose --project-directory . ${LOCAL_CONFIGS} up) || (echo "publish application with sbt application/docker:publishLocal!")
local-stop:
	docker-compose --project-directory . ${LOCAL_CONFIGS} stop
local-down:
	docker-compose --project-directory . ${LOCAL_CONFIGS} down --remove-orphans || \
	(docker container rm $(NETWORK_PREFIX)_kafka_1 $(NETWORK_PREFIX)_postgres_1 -f && \
	 docker network disconnect $(NETWORK_PREFIX)_branchtalk-monolith $(NETWORK_PREFIX)_kafka_1 -f && \
   docker network disconnect $(NETWORK_PREFIX)_branchtalk-monolith $(NETWORK_PREFIX)_postgres_1 -f && \
   docker network rm $(NETWORK_PREFIX)_branchtalk-monolith)
local-ps:
	docker-compose --project-directory . ${LOCAL_CONFIGS} ps
local-logs:
	docker-compose --project-directory . ${LOCAL_CONFIGS} logs -f ${LOGS}

clean-volumes:
	docker volume rm $(NETWORK_PREFIX)_postgres_data -f
	docker volume rm $(NETWORK_PREFIX)_kafka_data -f
	docker volume rm $(NETWORK_PREFIX)_zookeeper_data -f
