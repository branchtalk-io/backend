package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.discussions.model._
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait SubscriptionCommandEvent extends ADT
object SubscriptionCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Subscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime
  ) extends SubscriptionCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Unsubscribe(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime
  ) extends SubscriptionCommandEvent
}
