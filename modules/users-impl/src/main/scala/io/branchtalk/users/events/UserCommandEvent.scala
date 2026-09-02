package io.branchtalk.users.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.logging.CorrelationID
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.branchtalk.shared.model.AvroSupport.{ *, given }
import io.branchtalk.users.model.{ Password, Permission, Session, User }
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.partial.syntax.*

sealed trait UserCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object UserCommandEvent {

  // Encrypted variants are top-level subtypes (not nested `Create.Encrypted` inside a same-named sibling case class's
  // companion), which trips Kindlings' avro sum derivation - see UserEvent for the same reasoning.

  final case class Create(
    id:               ID[User],
    email:            SensitiveData[User.Email],
    username:         SensitiveData[User.Name],
    description:      Option[User.Description],
    password:         SensitiveData[Password],
    createdAt:        CreationTime,
    sessionID:        ID[Session],
    sessionExpiresAt: Session.ExpirationTime,
    correlationID:    CorrelationID
  ) derives FastEq,
        ShowPretty {

    def encrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): CreateEncrypted = this
      .into[CreateEncrypted]
      .withFieldComputed(_.email, _.email.encrypt(algorithm, key))
      .withFieldComputed(_.username, _.username.encrypt(algorithm, key))
      .withFieldComputed(_.password, _.password.encrypt(algorithm, key))
      .transform
  }

  final case class CreateEncrypted(
    id:               ID[User],
    email:            SensitiveData.Encrypted[User.Email],
    username:         SensitiveData.Encrypted[User.Name],
    description:      Option[User.Description],
    password:         SensitiveData.Encrypted[Password],
    createdAt:        CreationTime,
    sessionID:        ID[Session],
    sessionExpiresAt: Session.ExpirationTime,
    correlationID:    CorrelationID
  ) extends UserCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty {

    def decrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): DeserializationResult[Create] = this
      .intoPartial[Create]
      .withFieldComputedPartial(_.email, _.email.decrypt(algorithm, key).asResult)
      .withFieldComputedPartial(_.username, _.username.decrypt(algorithm, key).asResult)
      .withFieldComputedPartial(_.password, _.password.decrypt(algorithm, key).asResult)
      .transform
      .asEither
      .leftMap(e => DeserializationError.DecryptionError(this.show, e.asErrorPathMessageStrings.toMap))
  }

  final case class Update(
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
    ): UpdateEncrypted = this
      .into[UpdateEncrypted]
      .withFieldComputed(_.newUsername, _.newUsername.map(_.encrypt(algorithm, key)))
      .withFieldComputed(_.newPassword, _.newPassword.map(_.encrypt(algorithm, key)))
      .transform
  }

  final case class UpdateEncrypted(
    id:                ID[User],
    moderatorID:       Option[ID[User]],
    newUsername:       Updatable[SensitiveData.Encrypted[User.Name]],
    newDescription:    OptionUpdatable[User.Description],
    newPassword:       Updatable[SensitiveData.Encrypted[Password]],
    updatePermissions: List[Permission.Update],
    modifiedAt:        ModificationTime,
    correlationID:     CorrelationID
  ) extends UserCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty {

    def decrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): DeserializationResult[Update] = this
      .intoPartial[Update]
      .withFieldComputedPartial(_.newUsername, _.newUsername.traverse(_.decrypt(algorithm, key)).asResult)
      .withFieldComputedPartial(_.newPassword, _.newPassword.traverse(_.decrypt(algorithm, key)).asResult)
      .transform
      .asEither
      .leftMap(e => DeserializationError.DecryptionError(this.show, e.asErrorPathMessageStrings.toMap))
  }

  final case class Delete(
    id:            ID[User],
    moderatorID:   Option[ID[User]],
    deletedAt:     ModificationTime,
    correlationID: CorrelationID
  ) extends UserCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class RequestEmailUpdate(
    id:            ID[User],
    newEmail:      SensitiveData[User.Email],
    token:         User.EmailConfirmationToken,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) derives FastEq,
        ShowPretty {

    def encrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): RequestEmailUpdateEncrypted = this
      .into[RequestEmailUpdateEncrypted]
      .withFieldComputed(_.newEmail, _.newEmail.encrypt(algorithm, key))
      .transform
  }

  final case class RequestEmailUpdateEncrypted(
    id:            ID[User],
    newEmail:      SensitiveData.Encrypted[User.Email],
    token:         User.EmailConfirmationToken,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends UserCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty {

    def decrypt(
      algorithm: SensitiveData.Algorithm,
      key:       SensitiveData.Key
    ): DeserializationResult[RequestEmailUpdate] = this
      .intoPartial[RequestEmailUpdate]
      .withFieldComputedPartial(_.newEmail, _.newEmail.decrypt(algorithm, key).asResult)
      .transform
      .asEither
      .leftMap(e => DeserializationError.DecryptionError(this.show, e.asErrorPathMessageStrings.toMap))
  }

  final case class ConfirmEmail(
    id:            ID[User],
    token:         User.EmailConfirmationToken,
    modifiedAt:    ModificationTime,
    correlationID: CorrelationID
  ) extends UserCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
