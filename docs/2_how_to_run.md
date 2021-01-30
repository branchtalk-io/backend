# How to run BranchTalk

In order to run the application you have to create resources for read and write
models and point them in the application's configs.

## Write models

The project uses Kafka as its event bus (controversial decision on its own,
I know), so you need a Kafka cluster configured. Since Kafka by default doesn't
store messages indefinitely, you have to
[configure that yourself](https://www.confluent.io/blog/okay-store-data-apache-kafka/).
You would have to create separate topics:

 * for Users commands queue (used internally by Users)
 * for Users events (part of Users published language and internal API)
 * for Discussions commands queue (used internally by Discussions)
 * for Discussions events (part of Discussions published language and internal API)

Additionally, projections uses Redit to provide some idempotence (if offset wasn't
updated and application closed, the second processing of the event should use result
from the cache).

All these values could be configured by overriding
[HOCON configs](https://github.com/lightbend/config) via Java parameters:

```bash
# Example for docker-compose where kafka and redis are valid hosts.
# 0 indicated 0-th element on the list, if there were mode Kafka nodes
# each should have its own ...server.n.host=.../...server.n.post=...
# lines.
> export JAVA_OPTS="\
  -Dusers.published-event-bus.servers.0.host=kafka \
  -Dusers.published-event-bus.servers.0.port=9092 \
  -Dusers.published-event-bus.cache.host=redis \
  -Dusers.published-event-bus.cache.port=6379 \
  -Dusers.internal-event-bus.servers.0.host=kafka \
  -Dusers.internal-event-bus.servers.0.port=9092 \
  -Dusers.internal-event-bus.cache.host=redis \
  -Dusers.internal-event-bus.cache.port=6379 \
  -Ddiscussions.published-event-bus.servers.0.host=kafka \
  -Ddiscussions.published-event-bus.servers.0.port=9092 \
  -Ddiscussions.published-event-bus.cache.host=redis \
  -Ddiscussions.published-event-bus.cache.port=6379 \
  -Ddiscussions.internal-event-bus.servers.0.host=kafka \
  -Ddiscussions.internal-event-bus.servers.0.port=9092 \
  -Ddiscussions.internal-event-bus.cache.host=redis \
  -Ddiscussions.internal-event-bus.cache.port=6379 \
"
```

## Read models

Read models are stored in Postgres databases (if full context search will
ever be implemented there would be another read model database beside it).

Because of scalability we separated read Postgres instance (which we should
run projections into) and read instance (that might be some replica). Each
domain has its own dedicated database(s):

```bash
# Usually, you would also have to override username and password, see below.
> export JAVA_OPTS="\
  -Dusers.database-reads.url=jdbc:postgresql://postgres:5432/users \
  -Dusers.database-writes.url=jdbc:postgresql://postgres:5432/users \
  -Ddiscussions.database-reads.url=jdbc:postgresql://postgres:5432/discussions \
  -Ddiscussions.database-writes.url=jdbc:postgresql://postgres:5432/discussions \
"
```

Other options which you could configure for each
`*.database-reads`/`*.database-writes` are:

 * `username` - user accessing the DB
 * `password` - user's password
 * `connection-pool` - how many connections to DB should be allowed
 * `migration-on-start` - should the node run migrations
 * `schema` - database schema used

## Running modes

Once we have configurations prepared we can run by either passing them as Java
arguments:

```bash
# Java arguments have to be before -jar, because java commend interprets
# argument right after it as JAR name followed by app arguments.
> java $JAVA_OPTS -jar path/to/branchtalk.jar app-arguments
```

It is normally not necessary if you use standard `$JAVA_OPTS` environment
variable which passes arguments to `java` command.

Application without any arguments should close immediately. If we want it to
run, we should tell it what this particular instance should do:

 * `--api` argument tells it to start API server, which would only write
   commands to internal command queues and read from read models
 * `--users-projections` argument tells it to start Users projections which
   would read Users commands, convert them to events and then project events
   into read models
 * `--discussions-projections` argument tells it to start Discussions
   projections which would read Users commands, convert them to events and
   then project events into read models
 * `--monolith` argument tells it to run all the above at once in a
   single instance

This gives some flexibility when it comes to deciding how many nodes would
expose API and how many would take care of projections.

Kafka's consumer group should be responsible for dispatching commands and
events to different consumers.

When app receives `SIGTERM` or `SIGINT` signal, it should gracefully exit.

### Local demo

Monolith-mode can be demonstrated by running:

```bash
> ./sbt application/docker:publishLocal
> make local-up
```
or
```bash
> ./sbt application/docker:publishLocal
> make local-bg
> LOGS=application make local-logs
```
and then accessing
[OpenAPI](http://localhost:8080/docs/index.html?url=/docs/swagger.json).

## More options

More options are available. Please consult `--help` to learn about them.
