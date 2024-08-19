# common variables

set shell := ["zsh", "--login", "-c"]

network-prefix := `basename "$PWD"`
config-dir     := justfile_directory() + "/docker-compose"
dev-configs    := "--file " + config-dir + "/monolith-deps.yml --file " + config-dir + "/monolith-setup.yml"
local-configs  := dev-configs + " --file " + config-dir + "/monolith-app.yml"

# fetch dependencies
pull:
  docker compose --project-directory . {{dev-configs}} pull

# dev environment to use with sbt
dev-bg:
  docker compose --project-directory . {{dev-configs}} up --detach --quiet-pull
dev-up:
  docker compose --project-directory . {{dev-configs}} up
dev-stop:
  docker compose --project-directory . {{dev-configs}} stop
dev-down:
  docker-compose --project-directory . {{dev-configs}} down --remove-orphans || \
  (docker container rm {{network-prefix}}_kafka_1 {{network-prefix}}_postgres_1 -f && \
   docker network disconnect {{network-prefix}}_branchtalk-monolith {{network-prefix}}_kafka_1 -f && \
   docker network disconnect {{network-prefix}}_branchtalk-monolith {{network-prefix}}_postgres_1 -f && \
   docker network rm {{network-prefix}}_branchtalk-monolith)
dev-ps:
  docker compose --project-directory . {{dev-configs}} ps
dev-logs:
  docker compose --project-directory . {{dev-configs}} logs -f ${LOGS}

# whole monolithic app setup for e.g. local frontend development
local-bg:
  (docker compose --project-directory . {{local-configs}} up --detach --quiet-pull) || (echo "publish application with sbt application/docker:publishLocal!")
local-up:
  (docker compose --project-directory . {{local-configs}} up) || (echo "publish application with sbt application/docker:publishLocal!")
local-stop:
  docker compose --project-directory . {{local-configs}} stop
local-down:
  docker compose --project-directory . {{local-configs}} down --remove-orphans || \
  (docker container rm {{network-prefix}}_kafka_1 {{network-prefix}}_postgres_1 -f && \
   docker network disconnect {{network-prefix}}_branchtalk-monolith {{network-prefix}}_kafka_1 -f && \
   docker network disconnect {{network-prefix}}_branchtalk-monolith {{network-prefix}}_postgres_1 -f && \
   docker network rm {{network-prefix}}_branchtalk-monolith)
local-ps:
  docker compose --project-directory . {{local-configs}} ps
local-logs:
  docker compose --project-directory . {{local-configs}} logs -f ${LOGS}

clean-volumes:
  docker volume rm {{network-prefix}}_postgres_data -f
  docker volume rm {{network-prefix}}_kafka_data -f
  docker volume rm {{network-prefix}}_zookeeper_data -f
