package io.subfibers.discussions.models

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.shared.models._
import io.subfibers.users.models.User
import io.subfibers.shared.derivation.ShowPretty

@Semi(Eq, ShowPretty) final case class Post(
  id:   ID[Post],
  data: Post.Data
)
object Post extends PostProperties with PostCommands {

  @Semi(Eq, ShowPretty) final case class Data(
    authorID:       ID[User],
    title:          Post.Title,
    content:        Post.Content,
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
