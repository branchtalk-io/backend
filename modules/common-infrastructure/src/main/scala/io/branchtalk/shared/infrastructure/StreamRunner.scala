package io.branchtalk.shared.infrastructure

import cats.{ Monad, Semigroup }
import cats.effect.{ Async, Deferred, Resource }
import cats.effect.implicits._
import fs2.Stream
import io.branchtalk.shared.model.Logger

// Start Stream as a Fiber, close it gracefully when releasing the resource.
final case class StreamRunner[F[_]](asResource: Resource[F, Unit])
object StreamRunner {

  type FromConsumerStream[F[_], Event] = ConsumerStream[F, Event] => StreamRunner[F]

  // Creates a StreamRunner which should run a Stream in separate Fiber and interrupt it when closing the resource.
  def apply[F[_]: Async, A](streamToDrain: Stream[F, A]): StreamRunner[F] = StreamRunner {
    val logger = Logger.getLogger[F]

    Resource
      .eval(Deferred[F, Either[Throwable, Unit]])
      .flatMap { signal =>
        Resource.make(
          streamToDrain.interruptWhen(signal).compile.drain.uncancelable.start <* logger.debug(
            "Started stream in background"
          )
        )(fiber =>
          signal.complete(().asRight) >> logger.debug("Triggered kill-switch") >> fiber.join >> logger.debug(
            "Stream finished"
          )
        )
      }
      .void
      .handleErrorWith { error: Throwable =>
        Resource.eval(
          logger.error(error)("Error occurred before kill-switch was triggered") >> error.raiseError[F, Unit]
        )
      }
  }

  implicit def semigroup[F[_]: Monad]: Semigroup[StreamRunner[F]] =
    (a, b) => StreamRunner(a.asResource >> b.asResource)
}
