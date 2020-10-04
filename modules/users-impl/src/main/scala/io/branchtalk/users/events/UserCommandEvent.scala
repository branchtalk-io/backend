package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.shared.models._
import io.branchtalk.shared.models.AvroSupport._
import io.branchtalk.users.model.{ Password, Permission, User }
import io.scalaland.catnip.Semi

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait UserCommandEvent extends ADT
object UserCommandEvent {

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Create(
    id:          ID[User],
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    password:    Password,
    createdAt:   CreationTime
  ) extends UserCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Update(
    id:                ID[User],
    moderatorID:       Option[ID[User]],
    newUsername:       Updatable[User.Name],
    newDescription:    OptionUpdatable[User.Description],
    newPassword:       Updatable[Password],
    updatePermissions: List[Permission.Update],
    modifiedAt:        ModificationTime
  ) extends UserCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Delete(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  ) extends UserCommandEvent

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Restore(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  ) extends UserCommandEvent
}
