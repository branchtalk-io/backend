# branchtalk

Demo of how scalable implementation of Reddit-like service could be made
in Scala to demonstrate some patterns and principles.

## Status

Work-in-progress.

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
http://0.0.0.0:8080 and Swagger at http://0.0.0.0:8080/docs/index.html?url=/docs/swagger.json
