package io.branchtalk.discussions.events

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.models.{ Post, User }
import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty, Updatable }

@Semi(FastEq, ShowPretty) sealed trait PostCommandEvent extends ADT
object PostCommandEvent {

  @Semi(FastEq, ShowPretty) final case class Create(
    id:        ID[Post],
    authorID:  ID[User],
    title:     Post.Title,
    content:   Post.Content,
    createdAt: CreationTime
  ) extends PostCommandEvent

  @Semi(FastEq, ShowPretty) final case class Update(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content],
    modifiedAt: ModificationTime
  ) extends PostCommandEvent

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostCommandEvent
}
