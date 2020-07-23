package io.branchtalk.discussions.events

import com.sksamuel.avro4s.SchemaFor
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.shared.infrastructure.AvroSupport._
import io.branchtalk.shared.models.{ FastEq, ShowPretty }

@Semi(FastEq, ShowPretty, SchemaFor) sealed trait DiscussionCommandEvent extends ADT
object DiscussionCommandEvent {
  final case class ForChannel(channel: ChannelCommandEvent) extends DiscussionCommandEvent
  final case class ForComment(comment: CommentCommandEvent) extends DiscussionCommandEvent
  final case class ForPost(post:       PostCommandEvent) extends DiscussionCommandEvent
}
