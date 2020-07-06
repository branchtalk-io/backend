package io.subfibers.discussions.events

import cats.Eq
import io.scalaland.catnip.Semi
import io.subfibers.ADT
import io.subfibers.shared.derivation.ShowPretty

@Semi(Eq, ShowPretty) sealed trait DiscussionCommandEvent extends ADT
object DiscussionCommandEvent {
  final case class ForChannel(channel: ChannelCommandEvent) extends DiscussionCommandEvent
  final case class ForComment(comment: CommentCommandEvent) extends DiscussionCommandEvent
  final case class ForPost(post:       PostCommandEvent) extends DiscussionCommandEvent
}
