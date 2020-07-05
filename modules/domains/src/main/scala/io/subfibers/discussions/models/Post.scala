package io.subfibers.discussions.models

import io.estatico.newtype.macros.newtype
import io.subfibers.shared.models._
import io.subfibers.users.User

final case class Post(
  id:             ID[Post],
  authorID:       ID[User],
  title:          PostTitle,
  content:        PostContent,
  createdAt:      CreationTime,
  lastModifiedAt: Option[ModificationTime]
)
object Post {

  final case class Create(
    authorID: ID[User],
    title:    PostTitle,
    content:  PostContent
  )

  final case class Update(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Option[PostTitle],
    newContent: Option[PostContent]
  )

  @newtype case class Delete(value: ID[Post])
}
