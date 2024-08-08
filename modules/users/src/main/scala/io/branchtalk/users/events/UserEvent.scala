package io.branchtalk.users.events

import com.sksamuel.avro4s.*
import io.branchtalk.logging.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.branchtalk.users.model.{ Password, Permission, Session, User }
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.partial.syntax.*

// user events doesn't store any data as they can be sensitive data
sealed trait UserEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
object UserEvent {

  final case class Created(
    id:               ID[User],
    sessionID:        ID[Session], // session created by registration
    email:            SensitiveData[User.Email],
    username:         SensitiveData[User.Name],
    description:      Option[User.Description],
    password:         SensitiveData[Password],
    sessionExpiresAt: Session.ExpirationTime,
    createdAt:        CreationTime,
    correlationID:    CorrelationID
  ) derives FastEq,
        ShowPretty {

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

    final case class Encrypted(
      id:               ID[User],
      sessionID:        ID[Session], // session created by registration
      email:            SensitiveData.Encrypted[User.Email],
      username:         SensitiveData.Encrypted[User.Name],
      description:      Option[User.Description],
      password:         SensitiveData.Encrypted[Password],
      sessionExpiresAt: Session.ExpirationTime,
      createdAt:        CreationTime,
      correlationID:    CorrelationID
    ) extends UserEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor {

      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      ): DeserializationResult[Created] = this
        .intoPartial[Created]
        .withFieldComputedPartial(_.email, _.email.decrypt(algorithm, key).asResult)
        .withFieldComputedPartial(_.username, _.username.decrypt(algorithm, key).asResult)
        .withFieldComputedPartial(_.password, _.password.decrypt(algorithm, key).asResult)
        .transform
        .asEither
        .leftMap(e => DeserializationError.DecryptionError(this.show, e.asErrorPathMessageStrings.toMap))
    }
  }

  final case class Updated(
    id:                ID[User],
    moderatorID:       Option[ID[User]],
    newUsername:       Updatable[SensitiveData[User.Name]],
    newDescription:    OptionUpdatable[User.Description],
    newPassword:       Updatable[SensitiveData[Password]],
    updatePermissions: List[Permission.Update],
    modifiedAt:        ModificationTime,
    correlationID:     CorrelationID
  ) derives FastEq,
        ShowPretty {

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

    final case class Encrypted(
      id:                ID[User],
      moderatorID:       Option[ID[User]],
      newUsername:       Updatable[SensitiveData.Encrypted[User.Name]],
      newDescription:    OptionUpdatable[User.Description],
      newPassword:       Updatable[SensitiveData.Encrypted[Password]],
      updatePermissions: List[Permission.Update],
      modifiedAt:        ModificationTime,
      correlationID:     CorrelationID
    ) extends UserEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor {

      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      ): DeserializationResult[Updated] = this
        .intoPartial[Updated]
        .withFieldComputedPartial(_.newUsername, _.newUsername.traverse(_.decrypt(algorithm, key)).asResult)
        .withFieldComputedPartial(_.newPassword, _.newPassword.traverse(_.decrypt(algorithm, key)).asResult)
        .transform
        .asEither
        .leftMap(e => DeserializationError.DecryptionError(this.show, e.asErrorPathMessageStrings.toMap))
    }
  }

  final case class Deleted(
    id:            ID[User],
    moderatorID:   Option[ID[User]],
    deletedAt:     ModificationTime,
    correlationID: CorrelationID
  ) extends UserEvent derives Decoder, Encoder, FastEq, ShowPretty, SchemaFor
}
