package io.branchtalk.discussions.dao

import io.scalaland.catnip.Semi
import io.branchtalk.shared.models.{ FastEq, ID, ShowPretty, Updatable }

trait PostCommands { self: Post.type =>
  type Create = PostCommands.Create
  type Update = PostCommands.Update
  type Delete = PostCommands.Delete
  val Create = PostCommands.Create
  val Update = PostCommands.Update
  val Delete = PostCommands.Delete
}
object PostCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    authorID:  ID[User],
    channelID: ID[Channel],
    title:     Post.Title,
    content:   Post.Content
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

  @Semi(FastEq, ShowPretty) final case class Restore(
    id:       ID[Post],
    editorID: ID[User]
  )
}
