package io.subfibers.discussions.models

import java.net.URI

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.subfibers.shared.models._
import io.subfibers.users.models.User
import io.subfibers.ADT

final case class Post(
  id:             ID[Post],
  authorID:       ID[User],
  title:          Post.Title,
  content:        Post.Content,
  createdAt:      CreationTime,
  lastModifiedAt: Option[ModificationTime]
)
object Post {

  @newtype final case class Title(value: NonEmptyString)
  @newtype final case class URL(value:   URI)
  @newtype final case class Text(value:  String)

  sealed trait Content extends ADT
  object Content {
    final case class Url(url:   URL) extends Content
    final case class Text(text: Text) extends Content
  }

  final case class Create(
    authorID: ID[User],
    title:    Post.Title,
    content:  Content
  )

  final case class Update(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Option[Post.Title],
    newContent: Option[Content]
  )

  @newtype case class Delete(value: ID[Post])
}
