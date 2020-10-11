package io.branchtalk.discussions.model

import cats.{ Eq, Show }
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import io.branchtalk.shared.models.ParseRefined
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

trait CommentProperties { self: Comment.type =>
  type Content      = CommentProperties.Content
  type NestingLevel = CommentProperties.NestingLevel
  val Content      = CommentProperties.Content
  val NestingLevel = CommentProperties.NestingLevel
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
}
