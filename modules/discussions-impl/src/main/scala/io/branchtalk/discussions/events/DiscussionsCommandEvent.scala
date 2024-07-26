package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport.*

sealed trait DiscussionsCommandEvent derives Decoder, Encoder, ShowPretty, SchemaFor
object DiscussionsCommandEvent {
  final case class ForChannel(channel: ChannelCommandEvent) extends DiscussionsCommandEvent
  final case class ForComment(comment: CommentCommandEvent) extends DiscussionsCommandEvent
  final case class ForPost(post: PostCommandEvent) extends DiscussionsCommandEvent
  final case class ForSubscription(subscription: SubscriptionCommandEvent) extends DiscussionsCommandEvent
}
