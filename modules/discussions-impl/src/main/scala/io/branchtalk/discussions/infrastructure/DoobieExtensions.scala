package io.branchtalk.discussions.infrastructure

import io.branchtalk.discussions.model.{ Post, Vote }
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.branchtalkLocale

object DoobieExtensions {

  given postContentTypeMeta: Meta[Post.Content.Type] = pgEnumString(
    "post_content_type",
    name =>
      Post.Content.Type.values
        .find(_.entryName.equalsIgnoreCase(name))
        .getOrElse(
          throw new NoSuchElementException(
            s"$name is not a member of Enum (${Post.Content.Type.values.mkString(", ")})"
          )
        ),
    _.entryName.toLowerCase(branchtalkLocale)
  )

  given voteTypeMeta: Meta[Vote.Type] =
    pgEnumString("vote_type", Vote.Type.withNameInsensitive, _.entryName.toLowerCase(branchtalkLocale))
}
