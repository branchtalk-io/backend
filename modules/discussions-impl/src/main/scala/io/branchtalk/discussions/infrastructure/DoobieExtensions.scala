package io.branchtalk.discussions.infrastructure

import io.branchtalk.discussions.model.{ Post, Vote }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.branchtalkLocale

object DoobieExtensions {

  implicit val postContentTypeMeta: Meta[Post.Content.Type] =
    pgEnumString("post_content_type", Post.Content.Type.withNameInsensitive, _.entryName.toLowerCase(branchtalkLocale))

  implicit val voteTypeMeta: Meta[Vote.Type] =
    pgEnumString("vote_type", Vote.Type.withNameInsensitive, _.entryName.toLowerCase(branchtalkLocale))
}
