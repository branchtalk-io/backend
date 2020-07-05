package io.subfibers.discussions

import io.subfibers.ID
import io.subfibers.users.User

final case class Comment(
  id:            ID[Comment],
  authorID:      ID[User],
  commentedPost: ID[Post],
  replyTo:       Option[ID[Comment]],
  nestingLevel:  NestingLevel
)
