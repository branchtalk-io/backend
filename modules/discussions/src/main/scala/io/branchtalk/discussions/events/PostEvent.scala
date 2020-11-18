package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Post, User }
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait PostEvent extends ADT
object PostEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Created(
    id:        ID[Post],
    authorID:  ID[User],
    urlTitle:  Post.UrlTitle,
    title:     Post.Title,
    content:   Post.Content,
    createdAt: CreationTime
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Updated(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content],
    modifiedAt: ModificationTime
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Deleted(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restored(
    id:       ID[Post],
    editorID: ID[User]
  ) extends PostEvent
}
