package io.branchtalk.discussions.infrastructure

import cats.Show
import io.branchtalk.discussions.model.{ Post, Vote }
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.branchtalkLocale

object DoobieExtensions {

  private given [A: Show]: Show[Array[A]] = _.map(_.show).mkString(", ")

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  given postContentTypeMeta: Meta[Post.Content.Type] = pgEnumString(
    "post_content_type",
    name =>
      Post.Content.Type.values
        .find(_.entryName.equalsIgnoreCase(name))
        .getOrElse(throw new NoSuchElementException(show"$name is not a member of Enum (${Post.Content.Type.values})")),
    _.entryName.toLowerCase(branchtalkLocale)
  )

  given voteTypeMeta: Meta[Vote.Type] =
    pgEnumString("vote_type", Vote.Type.withNameInsensitive, _.entryName.toLowerCase(branchtalkLocale))
}
