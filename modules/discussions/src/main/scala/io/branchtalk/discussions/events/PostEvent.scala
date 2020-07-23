package io.branchtalk.discussions.events

import io.scalaland.catnip.Semi
import io.branchtalk.discussions.dao.{ Post, User }
import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty, Updatable }
import io.branchtalk.ADT

@Semi(FastEq, ShowPretty) sealed trait PostEvent extends ADT
object PostEvent {

  @Semi(FastEq, ShowPretty) final case class Created(
    id:        ID[Post],
    authorID:  ID[User],
    urlTitle:  Post.UrlTitle,
    title:     Post.Title,
    content:   Post.Content,
    createdAt: CreationTime
  ) extends PostEvent

  @Semi(FastEq, ShowPretty) final case class Updated(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content],
    modifiedAt: ModificationTime
  ) extends PostEvent

  @Semi(FastEq, ShowPretty) final case class Deleted(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostEvent

  @Semi(FastEq, ShowPretty) final case class Restored(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostEvent
}
