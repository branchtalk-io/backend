package io.subfibers.discussions

import java.net.URI

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

package object models {

  @newtype final case class PostTitle(value: NonEmptyString)

  @newtype final case class PostedURL(value:  URI)
  @newtype final case class PostedText(value: String)

  @newtype final case class CommentContent(value: String)

  @newtype final case class NestingLevel(value: Int Refined NonNegative)
}
