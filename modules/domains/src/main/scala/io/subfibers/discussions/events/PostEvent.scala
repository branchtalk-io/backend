package io.subfibers.discussions.events

import io.subfibers.ADT
import io.subfibers.discussions.models.Post
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.models.User

// TODO: modify all these events once we set up internal events

sealed trait PostEvent extends ADT
object PostEvent {

  sealed trait V1 extends PostEvent
  object V1 {

    final case class PostCreated(
      id:        ID[Post],
      authorID:  ID[User],
      title:     Post.Title,
      content:   Post.Content,
      createdAt: CreationTime
    ) extends V1

    final case class PostUpdated(
      id:             ID[Post],
      editorID:       ID[User],
      newTitle:       Option[Post.Title],
      newContent:     Option[Post.Content],
      lastModifiedAt: ModificationTime
    ) extends V1

    final case class PostDeleted(
      value: ID[Post]
    ) extends V1
  }
}
