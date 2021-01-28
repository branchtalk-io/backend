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

  // Create a stream that is emitting Units until you exit the resource.
  // Intended usage: zip stream  with event consumer on a separate thread, and stop consuming
  // when application terminates.
  def asStream[F[_]: Concurrent](withKillSwitchesStreamDrain: Stream[F, Unit] => F[Unit]): StreamRunner[F] =
    StreamRunner {
      val logger = Logger.getLogger[F]
      for {
        KillSwitch(killSwitchedStream, switchOff) <- Resource.liftF(apply[F])
        stream = withKillSwitchesStreamDrain(killSwitchedStream)
        _ <- Resource
          .make(stream.start >> logger.debug("Started stream in background"))(_ =>
            switchOff.start >> logger.debug("Triggered kill-switch") // TODO: figure out why fiber.join never finishes
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
