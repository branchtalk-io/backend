package io.branchtalk.discussions.model

import cats.{ Order, Show }
import cats.effect.Sync
import enumeratum._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import io.branchtalk.shared.model._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

trait CommentProperties { self: Comment.type =>
  type Content            = CommentProperties.Content
  type NestingLevel       = CommentProperties.NestingLevel
  type RepliesNr          = CommentProperties.RepliesNr
  type Upvotes            = CommentProperties.Upvotes
  type Downvotes          = CommentProperties.Downvotes
  type TotalScore         = CommentProperties.TotalScore
  type ControversialScore = CommentProperties.ControversialScore
  type Sorting            = CommentProperties.Sorting
  val Content            = CommentProperties.Content
  val NestingLevel       = CommentProperties.NestingLevel
  val RepliesNr          = CommentProperties.RepliesNr
  val Upvotes            = CommentProperties.Upvotes
  val Downvotes          = CommentProperties.Downvotes
  val TotalScore         = CommentProperties.TotalScore
  val ControversialScore = CommentProperties.ControversialScore
  val Sorting            = CommentProperties.Sorting
}
object CommentProperties {

  @newtype final case class Content(string: String)
  object Content {
    def unapply(content: Content): Option[String] = content.string.some

    implicit val show:  Show[Content]  = Show.wrap(_.string)
    implicit val order: Order[Content] = Order[String].coerce
  }

  @newtype final case class NestingLevel(nonNegativeInt: Int Refined NonNegative)
  object NestingLevel {
    def unapply(nestingLevel: NestingLevel): Option[Int Refined NonNegative] = nestingLevel.nonNegativeInt.some
    def parse[F[_]: Sync](int: Int): F[NestingLevel] =
      ParseRefined[F].parse[NonNegative](int).map(NestingLevel.apply)

    implicit val show:  Show[NestingLevel]  = Show.wrap(_.nonNegativeInt.value)
    implicit val order: Order[NestingLevel] = Order.by(_.nonNegativeInt.value)
  }

  @newtype final case class RepliesNr(toNonNegativeInt: Int Refined NonNegative)
  object RepliesNr {
    def unapply(repliesNr: RepliesNr): Option[Int Refined NonNegative] = repliesNr.toNonNegativeInt.some

    implicit val show:  Show[RepliesNr]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[RepliesNr] = Order.by(_.toNonNegativeInt.value)
  }

  @newtype final case class Upvotes(toNonNegativeInt: Int Refined NonNegative)
  object Upvotes {
    def unapply(upvotes: Upvotes): Option[Int Refined NonNegative] = upvotes.toNonNegativeInt.some

    implicit val show:  Show[Upvotes]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[Upvotes] = Order.by(_.toNonNegativeInt.value)
  }

  @newtype final case class Downvotes(toNonNegativeInt: Int Refined NonNegative)
  object Downvotes {
    def unapply(downvotes: Downvotes): Option[Int Refined NonNegative] = downvotes.toNonNegativeInt.some

    implicit val show:  Show[Downvotes]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[Downvotes] = Order.by(_.toNonNegativeInt.value)
  }

  @newtype final case class TotalScore(toInt: Int)
  object TotalScore {
    def unapply(totalScore: TotalScore): Option[Int] = totalScore.toInt.some

    implicit val show:  Show[TotalScore]  = Show.wrap(_.toInt)
    implicit val order: Order[TotalScore] = Order[Int].coerce
  }

  @newtype final case class ControversialScore(toNonNegativeInt: Int Refined NonNegative)
  object ControversialScore {
    def unapply(controversialScore: ControversialScore): Option[Int Refined NonNegative] =
      controversialScore.toNonNegativeInt.some

    implicit val show:  Show[ControversialScore]  = Show.wrap(_.toNonNegativeInt.value)
    implicit val order: Order[ControversialScore] = Order.by(_.toNonNegativeInt.value)
  }

  sealed trait Sorting extends EnumEntry
  object Sorting extends Enum[Sorting] {
    case object Newest extends Sorting
    case object TotalScore extends Sorting
    case object ControversialScore extends Sorting

    val values = findValues
  }
}
