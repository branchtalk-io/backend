package io.branchtalk.discussions.models

import io.scalaland.catnip.Semi
import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty, Updatable }

trait CommentCommands { self: Comment.type =>
  type Create = CommentCommands.Create
  type Update = CommentCommands.Update
  type Delete = CommentCommands.Delete
}
object CommentCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    authorID:      ID[User],
    commentedPost: ID[Post],
    content:       Comment.Content,
    replyTo:       Option[ID[Comment]]
  )

  @Semi(FastEq, ShowPretty) final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content]
  )

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Comment],
    editorID: ID[User]
  )
}
