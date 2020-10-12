package io.branchtalk.discussions.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.discussions.model.{ Channel, User }
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait SubscriptionEvent extends ADT
object SubscriptionEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Subscribed(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime
  ) extends SubscriptionEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Unsubscribed(
    subscriberID:  ID[User],
    subscriptions: Set[ID[Channel]],
    modifiedAt:    ModificationTime
  ) extends SubscriptionEvent
}
