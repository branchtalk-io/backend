package io.branchtalk.shared.infrastructure

import cats.effect.{ Resource, Sync }
import cats.effect.concurrent.Ref
import com.typesafe.scalalogging.Logger
import fs2._

final case class KillSwitch[F[_]](stream: Stream[F, Unit], switch: F[Unit])
object KillSwitch {

  private val logger = Logger(getClass)

  def apply[F[_]: Sync]: F[KillSwitch[F]] =
    Ref.of(true).map(switch => KillSwitch(Stream.repeatEval(switch.get).takeWhile(identity).void, switch.set(false)))

  // Create a stream that is emitting Units until you exit the resource.
  // Intended usage: zip stream  with event consumer on a separate thread, and stop consuming
  // when application terminates.
  def asStream[F[_]: Sync, A](f: Stream[F, Unit] => A): Resource[F, A] =
    Resource.make(apply[F])(_.switch).map(_.stream).map(f).handleErrorWith { error: Throwable =>
      logger.error("Error terminated computations before kill-switch was triggered", error)
      Resource.liftF(Sync[F].raiseError(error))
    }
}
