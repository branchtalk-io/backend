package io.branchtalk.discussions.models

import cats.{ Eq, Show }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

trait CommentProperties { self: Comment.type =>
  type Content      = CommentProperties.Content
  type NestingLevel = CommentProperties.NestingLevel
}
object CommentProperties {

  @newtype final case class Content(value: String)
  object Content {
    implicit val show: Show[Content] = /*_*/ Show[String].coerce /*_*/
    implicit val eq:   Eq[Content]   = /*_*/ Eq[String].coerce /*_*/
  }

  @newtype final case class NestingLevel(value: Int Refined NonNegative)
  object NestingLevel {
    implicit val show: Show[NestingLevel] = (t: NestingLevel) => s"NestingLevel(${t.value.value.show})"
    implicit val eq:   Eq[NestingLevel]   = (x: NestingLevel, y: NestingLevel) => x.value.value === y.value.value
  }
}
