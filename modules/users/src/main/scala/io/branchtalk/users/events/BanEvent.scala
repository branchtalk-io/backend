package io.branchtalk.users.events

import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.users.model.{ Ban, User }

sealed trait BanEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object BanEvent {

  final case class Banned(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    scope:         Ban.Scope,
    reason:        Ban.Reason,
    createdAt:     CreationTime,
    correlationID: CorrelationID
  ) extends BanEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor

  final case class Unbanned(
    bannedUserID:  ID[User],
    moderatorID:   Option[ID[User]],
    scope:         Ban.Scope,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends BanEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
