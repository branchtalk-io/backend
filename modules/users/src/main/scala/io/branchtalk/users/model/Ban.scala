package io.branchtalk.users.model

import cats.Show
import cats.effect.Sync
import enumeratum.*
import enumeratum.EnumEntry.Hyphencase
import io.branchtalk.shared.model.*

final case class Ban(
  bannedUserID: ID[User],
  reason:       Ban.Reason,
  scope:        Ban.Scope
) derives FastEq,
      ShowPretty
object Ban {

  final case class Order(
    bannedUserID: ID[User],
    reason:       Ban.Reason,
    scope:        Ban.Scope,
    moderatorID:  Option[ID[User]]
  ) derives FastEq,
        ShowPretty

  final case class Lift(
    bannedUserID: ID[User],
    scope:        Ban.Scope,
    moderatorID:  Option[ID[User]]
  ) derives FastEq,
        ShowPretty

  type Reason = Reason.Type
  object Reason extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(reason: Reason): Some[String] = Some(reason.unwrap)

    given Show[Reason]       = unsafeMakeF[Show](Show[String])
    given cats.Order[Reason] = unsafeMakeF[cats.Order](cats.Order[String])
  }

  enum Scope derives FastEq, ShowPretty {
    case ForChannel(channelID: ID[Channel])
    case Globally
  }
  object Scope {

    enum Type extends EnumEntry with Hyphencase derives FastEq, ShowPretty {
      case ForChannel
      case Globally
    }

    object Tupled {
      @SuppressWarnings(Array("org.wartremover.warts.Throw")) // illegal input from the DB
      def apply(scopeType: Type, scopeValue: Option[UUID]): Scope = (scopeType, scopeValue) match {
        case (Type.ForChannel, Some(uuid)) => Scope.ForChannel(ID[Channel](uuid))
        case (Type.Globally, _)            => Scope.Globally
        case _                             => throw new IllegalArgumentException("Expected ID for non-Global Scope")
      }

      def unpack(scope: Scope): (Type, Option[UUID]) = scope match {
        case ForChannel(channelID) => (Type.ForChannel, channelID.unwrap.some)
        case Globally              => (Type.Globally, none)
      }

      def unapply(scope: Scope): Some[(Type, Option[UUID])] = Some(unpack(scope))
    }
  }
}
