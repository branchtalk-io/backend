package io.subfibers.discussions.models

import cats.Eq
import cats.implicits._
import io.scalaland.catnip.Semi
import io.subfibers.shared.derivation.ShowPretty
import io.subfibers.shared.models.{ ID, Updatable }
import io.subfibers.users.models.User

trait PostCommands { self: Post.type =>
  type Create = PostCommands.Create
  type Update = PostCommands.Update
  type Delete = PostCommands.Delete
}
object PostCommands {

  @Semi(Eq, ShowPretty) final case class Create(
    authorID: ID[User],
    title:    Post.Title,
    content:  Post.Content
  )

  @Semi(Eq, ShowPretty) final case class Update(
    id:         ID[Post],
    editorID:   ID[User],
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content]
  )

  @Semi(Eq, ShowPretty) final case class Delete(
    id:       ID[Post],
    editorID: ID[User]
  )
}
