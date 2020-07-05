package io.subfibers.discussions.events

import io.subfibers.ADT
import io.subfibers.discussions.models.{ Comment, Post }
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.models.User

// TODO: modify all these events once we set up internal events

sealed trait DiscussionEvent extends ADT

sealed trait CommentEvent extends DiscussionEvent
object CommentEvent {

  sealed trait V1 extends CommentEvent
  object V1 {

    final case class CommentCreated(
      id:            ID[Comment],
      authorID:      ID[User],
      commentedPost: ID[Post],
      content:       Comment.Content,
      replyTo:       Option[ID[Comment]],
      createdAt:     CreationTime
    ) extends V1

    final case class CommentUpdated(
      id:             ID[Comment],
      editorID:       ID[User],
      newContent:     Option[Comment.Content],
      lastModifiedAt: ModificationTime
    ) extends V1

    final case class CommentDeleted(
      value: ID[Comment]
    ) extends V1
  }
}

sealed trait PostEvent extends DiscussionEvent
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
