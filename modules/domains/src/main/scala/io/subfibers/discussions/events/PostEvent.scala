package io.subfibers.discussions.events

import cats.Eq
import io.scalaland.catnip.Semi
import io.subfibers.discussions.models.Post
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime, Updatable }
import io.subfibers.users.models.User
import io.subfibers.ADT
import io.subfibers.shared.derivation.ShowPretty

@Semi(Eq, ShowPretty) sealed trait PostEvent extends ADT
object PostEvent {

  @Semi(Eq, ShowPretty) final case class Created(
    id:        ID[Post],
    authorID:  ID[User],
    title:     Post.Title,
    content:   Post.Content,
    createdAt: CreationTime
  ) extends PostEvent

  @Semi(Eq, ShowPretty) final case class Updated(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content],
    modifiedAt: ModificationTime
  ) extends PostEvent

  @Semi(Eq, ShowPretty) final case class Deleted(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostEvent
}
