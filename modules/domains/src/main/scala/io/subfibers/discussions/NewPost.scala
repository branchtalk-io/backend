package io.subfibers.discussions

import io.subfibers.ID
import io.subfibers.users.User

final case class NewPost(
  authorID: ID[User],
  title:    PostTitle,
  content:  PostContent
)
