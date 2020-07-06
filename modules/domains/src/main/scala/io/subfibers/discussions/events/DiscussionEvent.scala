package io.subfibers.discussions.events

import io.subfibers.ADT

sealed trait DiscussionEvent extends ADT
object DiscussionEvent {
  //final case class ForChannel(comment: CommentEvent) extends DiscussionEvent
  final case class ForComment(comment: CommentEvent) extends DiscussionEvent
  final case class ForPost(post:       PostEvent) extends DiscussionEvent
}
