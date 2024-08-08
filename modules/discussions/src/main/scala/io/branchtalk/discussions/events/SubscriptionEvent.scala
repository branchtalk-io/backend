package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait SubscriptionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object SubscriptionEvent {

  final case class Subscribed(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Unsubscribed(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
