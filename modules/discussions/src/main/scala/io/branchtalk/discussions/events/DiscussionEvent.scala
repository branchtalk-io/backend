package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait DiscussionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object DiscussionEvent {

  final case class ForChannel(
    channel: ChannelEvent
  ) extends DiscussionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class ForComment(
    comment: CommentEvent
  ) extends DiscussionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class ForPost(
    post: PostEvent
  ) extends DiscussionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class ForSubscription(
    subscription: SubscriptionEvent
  ) extends DiscussionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
