# Branchtalk

Demo of how scalable implementation of Reddit-like service could be made
in Scala to demonstrate some patterns and principles.

Goals and documentations are in [docs](docs/0_index.md) directory. You can also
start by looking at [main](modules/app/src/main/scala/io/branchtalk/Main.scala)
and browsing code from there in your IDE.

## Status

Achieved virtually all intended functionality:

 * users domain, with simple permission system and GDPR (deleting users from
   DB, events' fields containing personal data are encrypted)
 * discussions domain, with ability to subscribe to channels, having non-linear
   discussions, sorting posts by newest, most popular, most controversial
 * commands are enqueued and then turned into events, events are projected into
   updates of SQL database (it would be possible to add other projections)
 * API is documented using automatically generated Swagger
 * all important parameters are configurable, users can decide whether they
   want to run or not API, users' domain projections or discussions'
   projections in each instance

Some small TODOs are left, a few things could be improved or refactored. Code
could use some more documentation to explain design decisions and how utilities
work.

Things that could be implemented:

 * OAuth2 support
 * full context search using ElasticSearch or Solr or similar
 * some notification system

Things that are missing if this was a real project, but are out of scope
intended for example app:

 * reporting posts, comments and users to moderators
 * automatically detecting illegal content to remove it and block offenders
 * email validation

## Development

Building and testing requires java installed. Then

```bash
./sbt
```

downloads and runs sbt shell.

To run integration tests or local env you need docker and make:

```bash
make dev-bg   # starts services in background
make dev-up   # starts services in terminal (I suggest a separate tab/window)
```

Then it is possible to run it tests:

```bash
sbt> it:test
```

```bash
make dev-down # shuts down services
```

### Local testing

While in sbt with Docker started run
```bash
sbt:branchtalk> application/run --monolith
```
to start application in a fork. Ctrl+D to send shutdown signal.

OpenAPI will be available at:
```bash
http://localhost:8080/docs/index.html?url=/docs/swagger.json
```

### Running locally without sbt

Build docker image:
```bash
sbt:branchtalk> application/docker:publishLocal
```

Once the image is published you can:
```bash
make local-bg # or
make local-up
```
(If it fails I suggest running `make dev-down` before running local env).

You can use Ctrl+D to gracefully shutdown service. Server will be available at
http://localhost:8080 and Swagger at
http://localhost:8080/docs/index.html?url=/docs/swagger.json .
