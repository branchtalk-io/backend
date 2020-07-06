package io.subfibers.discussions.models

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.{ ID, Updatable }
import io.subfibers.users.models.User

trait CommentCommands { self: Comment.type =>
  type Create = CommentCommands.Create
  type Update = CommentCommands.Update
  type Delete = CommentCommands.Delete
}
object CommentCommands {

  @Semi(Eq, ShowPretty) final case class Create(
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]]
  )

  @Semi(Eq, ShowPretty) final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content]
  )

  @Semi(Eq, ShowPretty) final case class Delete(
    id:       ID[Comment],
    editorID: ID[User]
  )
}
