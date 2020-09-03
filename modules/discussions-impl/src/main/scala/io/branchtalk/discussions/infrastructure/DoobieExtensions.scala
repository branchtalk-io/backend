package io.branchtalk.discussions.infrastructure

import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.infrastructure.DoobieSupport._

object DoobieExtensions {

  implicit val postContentTypeMeta: Meta[Post.Content.Type] =
    pgEnumString("post_content_type", Post.Content.Type.withNameInsensitive, _.entryName.toLowerCase)
}
