package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait DiscussionsCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object DiscussionsCommandEvent {
  final case class ForChannel(channel: ChannelCommandEvent) extends DiscussionsCommandEvent
      derives AvroEncoder,
        AvroDecoder,
        FastEq,
        ShowPretty
  final case class ForComment(comment: CommentCommandEvent) extends DiscussionsCommandEvent
      derives AvroEncoder,
        AvroDecoder,
        FastEq,
        ShowPretty
  final case class ForPost(post: PostCommandEvent) extends DiscussionsCommandEvent
      derives AvroEncoder,
        AvroDecoder,
        FastEq,
        ShowPretty
  final case class ForSubscription(subscription: SubscriptionCommandEvent) extends DiscussionsCommandEvent
      derives AvroEncoder,
        AvroDecoder,
        FastEq,
        ShowPretty
}
