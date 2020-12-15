package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.scalaland.catnip.Semi
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._

@Semi(Decoder, Encoder, ShowPretty, SchemaFor) sealed trait DiscussionsCommandEvent extends ADT
object DiscussionsCommandEvent {
  final case class ForChannel(channel: ChannelCommandEvent) extends DiscussionsCommandEvent
  final case class ForComment(comment: CommentCommandEvent) extends DiscussionsCommandEvent
  final case class ForPost(post: PostCommandEvent) extends DiscussionsCommandEvent
  final case class ForSubscription(subscription: SubscriptionCommandEvent) extends DiscussionsCommandEvent
}
