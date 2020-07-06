package io.subfibers.discussions.events

import io.subfibers.ADT
import io.subfibers.discussions.models.{ Comment, Post }
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }
import io.subfibers.users.models.User

// TODO: modify all these events once we set up internal events

sealed trait DiscussionEvent extends ADT
object DiscussionEvent {
  final case class Comment(comment: CommentEvent) extends DiscussionEvent
  final case class Post(post:       PostEvent) extends DiscussionEvent
}
