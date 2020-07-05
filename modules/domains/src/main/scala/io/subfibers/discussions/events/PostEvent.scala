package io.subfibers.discussions.events

import io.subfibers.ADT
import io.subfibers.discussions.models.{ Post, PostContent, PostTitle }
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.User

sealed trait PostEvent extends ADT
object PostEvent {

  sealed trait V1 extends PostEvent
  object V1 {

    final case class PostCreated(
      id:        ID[Post],
      authorID:  ID[User],
      title:     PostTitle,
      content:   PostContent,
      createdAt: CreationTime
    ) extends V1

    final case class PostUpdated(
      id:             ID[Post],
      editorID:       ID[User],
      newTitle:       Option[PostTitle],
      newContent:     Option[PostContent],
      lastModifiedAt: ModificationTime
    ) extends V1

    final case class PostDeleted(
      value: ID[Post]
    ) extends V1
  }
}
