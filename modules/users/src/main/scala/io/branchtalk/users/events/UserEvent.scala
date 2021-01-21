package io.branchtalk.users.events

import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model._
import io.branchtalk.shared.model.AvroSupport._
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.branchtalk.users.model.{ Password, Permission, Session, User }
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._

// user events doesn't store any data as they can be sensitive data
@Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) sealed trait UserEvent extends ADT
object UserEvent {

  @Semi(FastEq, ShowPretty) final case class Created(
    id:               ID[User],
    sessionID:        ID[Session], // session created by registration
    email:            SensitiveData[User.Email],
    username:         SensitiveData[User.Name],
    description:      Option[User.Description],
    password:         SensitiveData[Password],
    sessionExpiresAt: Session.ExpirationTime,
    createdAt:        CreationTime,
    correlationID:    CorrelationID
  ) {

    def encrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): Created.Encrypted = this
      .into[Created.Encrypted]
      .withFieldComputed(_.email, _.email.encrypt(algorithm, key))
      .withFieldComputed(_.username, _.username.encrypt(algorithm, key))
      .withFieldComputed(_.password, _.password.encrypt(algorithm, key))
      .transform
  }
  object Created {

    @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Encrypted(
      id:               ID[User],
      sessionID:        ID[Session], // session created by registration
      email:            SensitiveData.Encrypted[User.Email],
      username:         SensitiveData.Encrypted[User.Name],
      description:      Option[User.Description],
      password:         SensitiveData.Encrypted[Password],
      sessionExpiresAt: Session.ExpirationTime,
      createdAt:        CreationTime,
      correlationID:    CorrelationID
    ) extends UserEvent {

      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      ): DeserializationResult[Created] = this
        .intoF[DeserializationResult, Created]
        .withFieldComputedF(_.email, _.email.decrypt(algorithm, key))
        .withFieldComputedF(_.username, _.username.decrypt(algorithm, key))
        .withFieldComputedF(_.password, _.password.decrypt(algorithm, key))
        .transform
    }
  }

  @Semi(FastEq, ShowPretty) final case class Updated(
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
    ): UserEvent.Updated.Encrypted = this
      .into[Updated.Encrypted]
      .withFieldComputed(_.newUsername, _.newUsername.map(_.encrypt(algorithm, key)))
      .withFieldComputed(_.newPassword, _.newPassword.map(_.encrypt(algorithm, key)))
      .transform
  }
  object Updated {

    @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Encrypted(
      id:                ID[User],
      moderatorID:       Option[ID[User]],
      newUsername:       Updatable[SensitiveData.Encrypted[User.Name]],
      newDescription:    OptionUpdatable[User.Description],
      newPassword:       Updatable[SensitiveData.Encrypted[Password]],
      updatePermissions: List[Permission.Update],
      modifiedAt:        ModificationTime,
      correlationID:     CorrelationID
    ) extends UserEvent {

      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      ): DeserializationResult[Updated] = this
        .intoF[DeserializationResult, Updated]
        .withFieldComputedF(_.newUsername, _.newUsername.traverse(_.decrypt(algorithm, key)))
        .withFieldComputedF(_.newPassword, _.newPassword.traverse(_.decrypt(algorithm, key)))
        .transform
    }
  }

  @Semi(Decoder, Encoder, FastEq, ShowPretty, SchemaFor) final case class Deleted(
    id:            ID[User],
    moderatorID:   Option[ID[User]],
    deletedAt:     ModificationTime,
    correlationID: CorrelationID
  ) extends UserEvent
}
