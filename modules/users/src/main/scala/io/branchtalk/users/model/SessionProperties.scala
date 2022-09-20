package io.branchtalk.users.model

import java.time.{ Instant, OffsetDateTime, ZoneId }
import java.time.format.DateTimeFormatter

import cats.{ Functor, Order, Show }
import cats.effect.Clock
import enumeratum.{ Enum, EnumEntry }
import enumeratum.EnumEntry.Hyphencase
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi

trait SessionProperties {
  type ExpirationTime = SessionProperties.ExpirationTime
  type Usage          = SessionProperties.Usage
  type Sorting        = SessionProperties.Sorting
  val ExpirationTime = SessionProperties.ExpirationTime
  val Usage          = SessionProperties.Usage
  val Sorting        = SessionProperties.Sorting
}
object SessionProperties {

  @newtype final case class ExpirationTime(offsetDateTime: OffsetDateTime) {

    def plusDays(days: Long): ExpirationTime = ExpirationTime(offsetDateTime.plusDays(days))
  }

  object ExpirationTime {
    def unapply(expirationTime: ExpirationTime): Some[OffsetDateTime] = Some(expirationTime.offsetDateTime)

    def now[F[_]: Functor: Clock]: F[ExpirationTime] =
      Clock[F].realTime
        .map(_.toMillis)
        .map(Instant.ofEpochMilli)
        .map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault()))
        .map(ExpirationTime(_))

    implicit val show: Show[ExpirationTime] = Show.wrap(_.offsetDateTime.pipe(DateTimeFormatter.ISO_INSTANT.format))
    implicit val order: Order[ExpirationTime] =
      Order.by[ExpirationTime, OffsetDateTime](_.offsetDateTime)(Order.fromComparable)
  }

  @Semi(FastEq, ShowPretty) sealed trait Usage extends ADT

  object Usage {
    case object UserSession extends Usage
    final case class OAuth(permissions: Permissions) extends Usage

    @Semi(FastEq, ShowPretty) sealed trait Type extends EnumEntry with Hyphencase
    object Type extends Enum[Type] {
      case object UserSession extends Type
      case object OAuth extends Type

      val values: IndexedSeq[Type] = findValues
    }

    object Tupled {
      def apply(usageType: Type, usagePermissions: Permissions): Usage = usageType match {
        case Type.UserSession => Usage.UserSession
        case Type.OAuth       => Usage.OAuth(usagePermissions)
      }

      def unpack(usage: Usage): (Type, Permissions) = usage match {
        case UserSession        => (Type.UserSession, Permissions(Set.empty))
        case OAuth(permissions) => (Type.OAuth, permissions)
      }

      def unapply(usage: Usage): Some[(Type, Permissions)] = Some(unpack(usage))
    }
  }

  sealed trait Sorting extends EnumEntry
  object Sorting extends Enum[Sorting] {
    case object ClosestToExpiry extends Sorting

    val values = findValues
  }
}
