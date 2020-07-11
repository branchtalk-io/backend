package io.branchtalk.discussions.events

import io.branchtalk.ADT
import io.branchtalk.shared.models.{ FastEq, ShowPretty }
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) sealed trait DiscussionEvent extends ADT
object DiscussionEvent {
  @Semi(FastEq, ShowPretty) final case class ForChannel(channel: ChannelEvent) extends DiscussionEvent
  @Semi(FastEq, ShowPretty) final case class ForComment(comment: CommentEvent) extends DiscussionEvent
  @Semi(FastEq, ShowPretty) final case class ForPost(post:       PostEvent) extends DiscussionEvent
}
