package io.branchtalk.discussions.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.discussions.model.*
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait SubscriptionCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object SubscriptionCommandEvent {

  final case class Subscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class Unsubscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
