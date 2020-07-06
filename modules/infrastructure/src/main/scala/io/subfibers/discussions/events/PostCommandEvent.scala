package io.subfibers.discussions.events

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.ADT
import io.subfibers.discussions.models.Post
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.models.User

@Semi(Eq, ShowPretty) sealed trait PostCommandEvent extends ADT
object PostCommandEvent {

  @Semi(Eq, ShowPretty) final case class Create(
    id:        ID[Post],
    authorID:  ID[User],
    title:     Post.Title,
    content:   Post.Content,
    createdAt: CreationTime
  ) extends PostCommandEvent

  @Semi(Eq, ShowPretty) final case class Update(
    id:             ID[Post],
    editorID:       ID[User],
    newTitle:       Option[Post.Title],
    newContent:     Option[Post.Content],
    lastModifiedAt: ModificationTime
  ) extends PostCommandEvent

  @Semi(Eq, ShowPretty) final case class Delete(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostCommandEvent
}
