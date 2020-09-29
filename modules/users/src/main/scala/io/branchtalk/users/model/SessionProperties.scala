package io.branchtalk.users.model

import java.time.{ Instant, OffsetDateTime, ZoneId }
import java.time.format.DateTimeFormatter

import cats.{ Functor, Order, Show }
import cats.effect.Clock
import io.branchtalk.ADT
import io.branchtalk.shared.models.{ FastEq, ShowPretty }
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi

trait SessionProperties {
  type ExpirationTime = SessionProperties.ExpirationTime
  type Type           = SessionProperties.Type
  val ExpirationTime = SessionProperties.ExpirationTime
  val Type           = SessionProperties.Type
}
object SessionProperties {

  @newtype final case class ExpirationTime(value: OffsetDateTime)
  object ExpirationTime {
    implicit val show: Show[ExpirationTime] =
      (t: ExpirationTime) => s"CreationTime(${DateTimeFormatter.ISO_INSTANT.format(t.value)})"
    implicit val order: Order[ExpirationTime] =
      (x: ExpirationTime, y: ExpirationTime) => x.value.compareTo(y.value)

    def now[F[_]: Functor: Clock]: F[ExpirationTime] =
      Clock[F]
        .realTime(scala.concurrent.duration.MILLISECONDS)
        .map(Instant.ofEpochMilli)
        .map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault()))
        .map(ExpirationTime(_))
  }

  @Semi(FastEq, ShowPretty) sealed trait Type extends ADT
  object Type {

    case object UserSession extends Type
    final case class OAuth(permissions: Permissions) extends Type
    // TODO: investigate if oauth-specific permissions won't work out better here
  }
}
