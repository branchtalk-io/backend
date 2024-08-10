package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.discussions.model.*
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait SubscriptionCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object SubscriptionCommandEvent {

  final case class Subscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Unsubscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionCommandEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
