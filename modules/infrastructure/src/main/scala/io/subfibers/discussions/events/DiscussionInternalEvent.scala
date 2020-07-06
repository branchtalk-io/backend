package io.subfibers.discussions.events

import io.subfibers.ADT

sealed trait DiscussionInternalEvent extends ADT
object DiscussionInternalEvent {
  final case class Comment(comment: CommentInternalEvent) extends DiscussionInternalEvent
  final case class Post(post:       PostInternalEvent) extends DiscussionInternalEvent
}
