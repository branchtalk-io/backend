package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.branchtalk.shared.model.AvroSupport._
import io.branchtalk.users.model.{ Password, Permission, Session, User }
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._

@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait UserCommandEvent extends ADT
object UserCommandEvent {

  @Semi(FastEq, ShowPretty) final case class Create(
    id:               ID[User],
    email:            SensitiveData[User.Email],
    username:         SensitiveData[User.Name],
    description:      Option[User.Description],
    password:         SensitiveData[Password],
    createdAt:        CreationTime,
    sessionID:        ID[Session],
    sessionExpiresAt: Session.ExpirationTime,
    correlationID:    CorrelationID
  ) {

    def encrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): Create.Encrypted = this
      .into[Create.Encrypted]
      .withFieldComputed(_.email, _.email.encrypt(algorithm, key))
      .withFieldComputed(_.username, _.username.encrypt(algorithm, key))
      .withFieldComputed(_.password, _.password.encrypt(algorithm, key))
      .transform
  }
  object Create {

    @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Encrypted(
      id:               ID[User],
      email:            SensitiveData.Encrypted[User.Email],
      username:         SensitiveData.Encrypted[User.Name],
      description:      Option[User.Description],
      password:         SensitiveData.Encrypted[Password],
      createdAt:        CreationTime,
      sessionID:        ID[Session],
      sessionExpiresAt: Session.ExpirationTime,
      correlationID:    CorrelationID
    ) extends UserCommandEvent {

      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      ): DeserializationResult[Create] = this
        .intoF[DeserializationResult, Create]
        .withFieldComputedF(_.email, _.email.decrypt(algorithm, key))
        .withFieldComputedF(_.username, _.username.decrypt(algorithm, key))
        .withFieldComputedF(_.password, _.password.decrypt(algorithm, key))
        .transform
    }
  }

  @Semi(FastEq, ShowPretty) final case class Update(
    id:                ID[User],
    moderatorID:       Option[ID[User]],
    newUsername:       Updatable[SensitiveData[User.Name]],
    newDescription:    OptionUpdatable[User.Description],
    newPassword:       Updatable[SensitiveData[Password]],
    updatePermissions: List[Permission.Update],
    modifiedAt:        ModificationTime,
    correlationID:     CorrelationID
  ) {

    def encrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): Update.Encrypted = this
      .into[Update.Encrypted]
      .withFieldComputed(_.newUsername, _.newUsername.map(_.encrypt(algorithm, key)))
      .withFieldComputed(_.newPassword, _.newPassword.map(_.encrypt(algorithm, key)))
      .transform
  }
  object Update {

    @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Encrypted(
      id:                ID[User],
      moderatorID:       Option[ID[User]],
      newUsername:       Updatable[SensitiveData.Encrypted[User.Name]],
      newDescription:    OptionUpdatable[User.Description],
      newPassword:       Updatable[SensitiveData.Encrypted[Password]],
      updatePermissions: List[Permission.Update],
      modifiedAt:        ModificationTime,
      correlationID:     CorrelationID
    ) extends UserCommandEvent {

      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      ): DeserializationResult[Update] = this
        .intoF[DeserializationResult, Update]
        .withFieldComputedF(_.newUsername, _.newUsername.traverse(_.decrypt(algorithm, key)))
        .withFieldComputedF(_.newPassword, _.newPassword.traverse(_.decrypt(algorithm, key)))
        .transform
    }
  }

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Delete(
    id:            ID[User],
    moderatorID:   Option[ID[User]],
    deletedAt:     ModificationTime,
    correlationID: CorrelationID
  ) extends UserCommandEvent
}
