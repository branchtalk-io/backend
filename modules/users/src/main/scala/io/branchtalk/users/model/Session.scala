package io.branchtalk.users.model

import java.time.{ Instant, OffsetDateTime, ZoneId }
import java.time.format.DateTimeFormatter

import cats.{ Functor, Order, Show }
import cats.effect.Clock
import enumeratum.{ Enum, EnumEntry }
import enumeratum.EnumEntry.Hyphencase
import io.branchtalk.shared.model.*

final case class Session(
  id:   ID[Session],
  data: Session.Data
) derives FastEq,
      ShowPretty
object Session {

  final case class Data(
    userID:    ID[User],
    usage:     Session.Usage,
    expiresAt: Session.ExpirationTime,
    ipAddress: Option[Session.IpAddress],
    userAgent: Option[Session.UserAgent]
  ) derives FastEq,
        ShowPretty

  final case class Create(
    userID:    ID[User],
    usage:     Session.Usage,
    expiresAt: Session.ExpirationTime,
    ipAddress: Option[Session.IpAddress],
    userAgent: Option[Session.UserAgent]
  ) derives FastEq,
        ShowPretty

  final case class Delete(
    id: ID[Session]
  ) derives FastEq,
        ShowPretty

  type IpAddress = IpAddress.Type
  object IpAddress extends Newtype[String] {
    def unapply(ipAddress: IpAddress): Some[String] = Some(ipAddress.unwrap)

    given Show[IpAddress]  = unsafeMakeF[Show](Show[String])
    given Order[IpAddress] = unsafeMakeF[Order](Order[String])
  }

  type UserAgent = UserAgent.Type
  object UserAgent extends Newtype[String] {
    def unapply(userAgent: UserAgent): Some[String] = Some(userAgent.unwrap)

    given Show[UserAgent]  = unsafeMakeF[Show](Show[String])
    given Order[UserAgent] = unsafeMakeF[Order](Order[String])
  }

  type ExpirationTime = ExpirationTime.Type
  object ExpirationTime extends Newtype[OffsetDateTime] {

    def unapply(expirationTime: ExpirationTime): Some[OffsetDateTime] = Some(expirationTime.unwrap)

    def now[F[_]: Functor: Clock]: F[ExpirationTime] =
      Clock[F].realTime
        .map(_.toMillis)
        .map(Instant.ofEpochMilli)
        .map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault()))
        .pipe(unsafeMakeF)

    given Show[ExpirationTime]  = _.unwrap.pipe(DateTimeFormatter.ISO_INSTANT.format)
    given Order[ExpirationTime] = Order.by[ExpirationTime, OffsetDateTime](_.unwrap)(Order.fromComparable)

    extension (time: ExpirationTime) {
      def plusDays(days: Long): ExpirationTime = ExpirationTime(time.unwrap.plusDays(days))
    }
  }

  enum Usage derives FastEq, ShowPretty {
    case UserSession
    case OAuth(permissions: Permissions)
  }
  object Usage {

    enum Type extends EnumEntry with Hyphencase derives FastEq, ShowPretty {
      case UserSession
      case OAuth
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

  enum Sorting derives FastEq, ShowPretty {
    case ClosestToExpiry
  }
}
