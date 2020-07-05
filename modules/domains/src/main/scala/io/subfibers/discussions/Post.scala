package io.subfibers.discussions

import io.subfibers._
import io.subfibers.users.User

final case class Post(
  id:             ID[Post],
  authorID:       ID[User],
  title:          PostTitle,
  content:        PostContent,
  createdAt:      CreationTime,
  lastModifiedAt: Option[ModificationTime]
)
