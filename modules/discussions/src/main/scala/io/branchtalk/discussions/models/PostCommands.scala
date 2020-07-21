package io.branchtalk.discussions.models

import io.scalaland.catnip.Semi
import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty, Updatable }

trait PostCommands { self: Post.type =>
  type Create = PostCommands.Create
  type Update = PostCommands.Update
  type Delete = PostCommands.Delete
}
object PostCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    authorID: ID[User],
    title:    Post.Title,
    content:  Post.Content
  )

  @Semi(FastEq, ShowPretty) final case class Update(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content]
  )

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Post],
    editorID: ID[User]
  )
}
