package io.branchtalk.discussions.events

import com.sksamuel.avro4s.SchemaFor
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Post, User }
import io.branchtalk.shared.infrastructure.AvroSupport._
import io.branchtalk.shared.models.{ CreationTime, FastEq, ID, ModificationTime, ShowPretty, Updatable }

@Semi(FastEq, ShowPretty, SchemaFor) sealed trait PostCommandEvent extends ADT
object PostCommandEvent {

  @Semi(FastEq, ShowPretty) final case class Create(
    id:        ID[Post],
    authorID:  ID[User],
    urlTitle:  Post.UrlTitle,
    title:     Post.Title,
    content:   Post.Content,
    createdAt: CreationTime
  ) extends PostCommandEvent

  @Semi(FastEq, ShowPretty) final case class Update(
    id:          ID[Post],
    editorID:    ID[User],
    newUrlTitle: Updatable[Post.UrlTitle],
    newTitle:    Updatable[Post.Title],
    newContent:  Updatable[Post.Content],
    modifiedAt:  ModificationTime
  ) extends PostCommandEvent

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostCommandEvent

  @Semi(FastEq, ShowPretty) final case class Restore(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostCommandEvent
}
