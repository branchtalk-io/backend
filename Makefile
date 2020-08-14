.EXPORT_ALL_VARIABLES:

CONFIG_DIR=docker-compose
DEV_CONFIGS=--file ${CONFIG_DIR}/monolith-deps.yml --file ${CONFIG_DIR}/monolith-setup.yml

pull:
	docker-compose --project-directory . ${DEV_CONFIGS} pull

dev-up:
	docker-compose --project-directory . ${DEV_CONFIGS} up
dev-down:
	docker-compose --project-directory . ${DEV_CONFIGS} down --remove-orphans
dev-ps:
	docker-compose --project-directory . ${DEV_CONFIGS} ps
dev-logs:
	docker-compose --project-directory . ${DEV_CONFIGS} logs ${LOGS}
