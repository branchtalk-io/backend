package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._
import io.branchtalk.ADT
import io.branchtalk.users.model.{ Session, User }
import io.scalaland.catnip.Semi

// user events doesn't store any data as they can be sensitive data
@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait UserEvent extends ADT
object UserEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Created(
    id:               ID[User],
    createdAt:        CreationTime,
    sessionID:        ID[Session], // session created by registration
    sessionExpiresAt: Session.ExpirationTime
  ) extends UserEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Updated(
    id:          ID[User],
    moderatorID: Option[ID[User]],
    modifiedAt:  ModificationTime
  ) extends UserEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Deleted(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  ) extends UserEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restored(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  ) extends UserEvent
}
