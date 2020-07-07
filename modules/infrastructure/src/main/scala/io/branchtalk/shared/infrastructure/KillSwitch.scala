package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2._

final case class KillSwitch[F[_]](stream: Stream[F, Unit], switch: F[Unit])
object KillSwitch {

  def apply[F[_]: Sync]: F[KillSwitch[F]] =
    Ref.of(true).map(switch => KillSwitch(Stream.repeatEval(switch.get).takeWhile(identity).void, switch.set(false)))
}
