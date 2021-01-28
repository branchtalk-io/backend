package io.branchtalk.shared.infrastructure

import cats.{ Monad, Semigroup }
import cats.effect.Resource

// Start Stream as a Fiber, close it gracefully when releasing the resource.
final case class StreamRunner[F[_]](asFiberResource: Resource[F, Unit])
object StreamRunner {

  type FromConsumerStream[F[_], Event] = ConsumerStream[F, Event] => StreamRunner[F]

  implicit def semigroup[F[_]: Monad]: Semigroup[StreamRunner[F]] =
    (a, b) => StreamRunner(a.asFiberResource >> b.asFiberResource)
}
