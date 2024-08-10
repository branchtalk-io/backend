package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait DiscussionsCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object DiscussionsCommandEvent {
  final case class ForChannel(channel: ChannelCommandEvent) extends DiscussionsCommandEvent
      derives Decoder,
        Encoder,
        FastEq,
        ShowPretty,
        SchemaFor
  final case class ForComment(comment: CommentCommandEvent) extends DiscussionsCommandEvent
      derives Decoder,
        Encoder,
        FastEq,
        ShowPretty,
        SchemaFor
  final case class ForPost(post: PostCommandEvent) extends DiscussionsCommandEvent
      derives Decoder,
        Encoder,
        FastEq,
        ShowPretty,
        SchemaFor
  final case class ForSubscription(subscription: SubscriptionCommandEvent) extends DiscussionsCommandEvent
      derives Decoder,
        Encoder,
        FastEq,
        ShowPretty,
        SchemaFor
}
