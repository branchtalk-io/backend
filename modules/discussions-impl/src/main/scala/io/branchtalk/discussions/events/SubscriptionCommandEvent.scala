package io.branchtalk.discussions.events

import com.sksamuel.avro4s.*
import io.branchtalk.ADT
import io.branchtalk.discussions.model.*
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.*
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait SubscriptionCommandEvent extends ADT
object SubscriptionCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Subscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Unsubscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends SubscriptionCommandEvent
}
