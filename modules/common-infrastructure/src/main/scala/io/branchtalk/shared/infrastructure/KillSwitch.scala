package io.branchtalk.shared.infrastructure

import cats.effect.{ Concurrent, Resource, Sync }
import cats.effect.concurrent.Ref
import cats.effect.implicits._
import fs2.{ io => _, _ }
import io.branchtalk.shared.model.Logger

final case class KillSwitch[F[_]](killSwitchedStream: Stream[F, Unit], switchOff: F[Unit])
object KillSwitch {

  def apply[F[_]: Sync]: F[KillSwitch[F]] =
    Ref.of(true).map(switch => KillSwitch(Stream.repeatEval(switch.get).takeWhile(identity).void, switch.set(false)))

  // Creates a StreamRunner which should run Stream in separate Fiber and trigger kill-switch when closing the resource.
  def streamToRunner[F[_]: Concurrent, A](streamToDrain: Stream[F, A]): StreamRunner[F] = StreamRunner {
    val logger = Logger.getLogger[F]
    for {
      KillSwitch(killSwitchedStream, switchOff) <- Resource.liftF(apply[F])
      stream = killSwitchedStream.zipRight(streamToDrain).compile.drain
      _ <- Resource
        .make(stream.start <* logger.debug("Started stream in background"))(fiber =>
          switchOff >> logger.debug("Triggered kill-switch") >> fiber.join
        )
        .void
        .handleErrorWith { error: Throwable =>
          Resource.liftF(
            logger.error(error)("Error occurred before kill-switch was triggered") >> error.raiseError[F, Unit]
          )
        }
    } yield ()
  }
}
