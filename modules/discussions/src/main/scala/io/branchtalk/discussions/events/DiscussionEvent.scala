package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait DiscussionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object DiscussionEvent {

  final case class ForChannel(
    channel: ChannelEvent
  ) extends DiscussionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class ForComment(
    comment: CommentEvent
  ) extends DiscussionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class ForPost(
    post: PostEvent
  ) extends DiscussionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class ForSubscription(
    subscription: SubscriptionEvent
  ) extends DiscussionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
