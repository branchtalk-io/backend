package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._

@Semi(Decoder, Encoder, ShowPretty, SchemaFor) sealed trait DiscussionCommandEvent extends ADT
object DiscussionCommandEvent {
  final case class ForChannel(channel:           ChannelCommandEvent) extends DiscussionCommandEvent
  final case class ForComment(comment:           CommentCommandEvent) extends DiscussionCommandEvent
  final case class ForPost(post:                 PostCommandEvent) extends DiscussionCommandEvent
  final case class ForSubscription(subscription: SubscriptionCommandEvent) extends DiscussionCommandEvent
}
