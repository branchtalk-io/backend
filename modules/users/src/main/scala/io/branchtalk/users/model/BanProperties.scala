package io.branchtalk.users.model

import cats.{ Order, Show }
import cats.effect.Sync
import enumeratum._
import enumeratum.EnumEntry.Hyphencase
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi

trait BanProperties {
  type Reason = BanProperties.Reason
  type Scope  = BanProperties.Scope
  val Reason = BanProperties.Reason
  val Scope  = BanProperties.Scope
}
object BanProperties {

  @newtype final case class Reason(nonEmptyString: NonEmptyString)
  object Reason {
    def unapply(reason: Reason): Option[NonEmptyString] = reason.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Reason] = ParseRefined[F].parse[NonEmpty](string).map(Reason.apply)

    implicit val show:  Show[Reason]  = Show.wrap(_.nonEmptyString.value)
    implicit val order: Order[Reason] = Order.by(_.nonEmptyString.value)
  }

  @Semi(FastEq, ShowPretty) sealed trait Scope extends ADT
  object Scope {
    final case class ForChannel(channelID: ID[Channel]) extends Scope
    case object Globally extends Scope

    @Semi(FastEq, ShowPretty) sealed trait Type extends EnumEntry with Hyphencase
    object Type extends Enum[Type] {
      case object ForChannel extends Type
      case object Globally extends Type

      val values: IndexedSeq[Type] = findValues
    }

    object Tupled {
      @SuppressWarnings(Array("org.wartremover.warts.Throw")) // illegal input from the DB
      def apply(scopeType: Type, scopeValue: Option[UUID]): Scope = (scopeType, scopeValue) match {
        case (Type.ForChannel, Some(uuid)) => Scope.ForChannel(ID[Channel](uuid))
        case (Type.Globally, _)            => Scope.Globally
        case _                             => throw new IllegalArgumentException("Expected ID for non-Global Scope")
      }

      def unpack(scope: Scope): (Type, Option[UUID]) = scope match {
        case ForChannel(channelID) => (Type.ForChannel, channelID.uuid.some)
        case Globally              => (Type.Globally, none)
      }

      def unapply(scope: Scope): Option[(Type, Option[UUID])] = unpack(scope).some
    }
  }
}
