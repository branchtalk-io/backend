package io.branchtalk.discussions.model

import cats.{ Eq, Order, Show }
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import io.branchtalk.shared.model.ParseRefined
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

trait CommentProperties { self: Comment.type =>
  type Content      = CommentProperties.Content
  type NestingLevel = CommentProperties.NestingLevel
  type RepliesNr    = CommentProperties.RepliesNr
  val Content      = CommentProperties.Content
  val NestingLevel = CommentProperties.NestingLevel
  val RepliesNr    = CommentProperties.RepliesNr
}
object CommentProperties {

  @newtype final case class Content(string: String)
  object Content {
    def unapply(content: Content): Option[String] = content.string.some

    implicit val show: Show[Content] = /*_*/ Show[String].coerce /*_*/
    implicit val eq:   Eq[Content]   = /*_*/ Eq[String].coerce /*_*/
  }

  @newtype final case class NestingLevel(nonNegativeInt: Int Refined NonNegative)
  object NestingLevel {
    def unapply(nestingLevel: NestingLevel): Option[Int Refined NonNegative] = nestingLevel.nonNegativeInt.some
    def parse[F[_]: Sync](int: Int): F[NestingLevel] =
      ParseRefined[F].parse[NonNegative](int).map(NestingLevel.apply)

    implicit val show: Show[NestingLevel] = (t: NestingLevel) => s"NestingLevel(${t.nonNegativeInt.value.show})"
    implicit val eq: Eq[NestingLevel] = (x: NestingLevel, y: NestingLevel) =>
      x.nonNegativeInt.value === y.nonNegativeInt.value
  }

  @newtype final case class RepliesNr(toNonNegativeInt: Int Refined NonNegative)
  object RepliesNr {
    def unapply(repliesNr: RepliesNr): Option[Int Refined NonNegative] = repliesNr.toNonNegativeInt.some

    implicit val show: Show[RepliesNr] = (t: RepliesNr) => t.toNonNegativeInt.value.toString
    implicit val order: Order[RepliesNr] = (x: RepliesNr, y: RepliesNr) =>
      x.toNonNegativeInt.value compareTo y.toNonNegativeInt.value
  }
}
