package io.subfibers.discussions

import io.subfibers.ID
import io.subfibers.users.User

final case class UpdatePost(
  id:         ID[Post],
  editor:     ID[User],
  newTitle:   Option[PostTitle],
  newContent: Option[PostContent]
)
