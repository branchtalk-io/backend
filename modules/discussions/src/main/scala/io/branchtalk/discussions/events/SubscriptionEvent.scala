package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait SubscriptionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object SubscriptionEvent {

  final case class Subscribed(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Unsubscribed(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
