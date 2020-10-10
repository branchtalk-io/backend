package io.branchtalk.users.api

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import io.branchtalk.ADT
import io.branchtalk.api.{ Password => _, Permission => _, _ }
import io.branchtalk.shared.models.ID
import io.branchtalk.users.model._
import io.scalaland.catnip.Semi
import sttp.tapir.Schema

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object UserModels {

  // properties codecs
  implicit val userEmailCodec: JsCodec[User.Email] =
    summonCodec[String](JsonCodecMaker.make).refine[MatchesRegex["(.+)@(.+)"]].asNewtype[User.Email]
  implicit val usernameCodec: JsCodec[User.Name] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[User.Name]
  implicit val userDescriptionCodec: JsCodec[User.Description] =
    summonCodec[String](JsonCodecMaker.make).asNewtype[User.Description]
  implicit val passwordHashCodec: JsCodec[Password.Hash] =
    summonCodec[Array[Byte]](JsonCodecMaker.make).asNewtype[Password.Hash]
  implicit val passwordSaltCodec: JsCodec[Password.Salt] =
    summonCodec[Array[Byte]](JsonCodecMaker.make).asNewtype[Password.Salt]
  implicit val passwordRawCodec: JsCodec[Password.Raw] =
    summonCodec[Array[Byte]](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Password.Raw]
  implicit val permissionsCodec: JsCodec[Permissions] =
    summonCodec[Set[Permission]](JsonCodecMaker.make).asNewtype[Permissions]

  // properties schemas
  implicit val userEmailSchema: Schema[User.Email] =
    summonSchema[String Refined MatchesRegex["(.+)@(.+)"]].asNewtype[User.Email]
  implicit val usernameSchema: Schema[User.Name] =
    summonSchema[String Refined NonEmpty].asNewtype[User.Name]
  implicit val userDescriptionSchema: Schema[User.Description] =
    summonSchema[String].asNewtype[User.Description]
  implicit val passwordHashSchema: Schema[Password.Hash] =
    summonSchema[Array[Byte]].asNewtype[Password.Hash]
  implicit val passwordSaltSchema: Schema[Password.Salt] =
    summonSchema[Array[Byte]].asNewtype[Password.Salt]
  implicit val passwordRawSchema: Schema[Password.Raw] =
    summonSchema[Array[Byte] Refined NonEmpty].asNewtype[Password.Raw]
  implicit val permissionsSchema: Schema[Permissions] =
    summonSchema[Set[Permission]].asNewtype[Permissions]

  @Semi(JsCodec) sealed trait UserError extends ADT
  object UserError {

    final case class BadCredentials(msg:     String) extends UserError
    final case class NotFound(msg:           String) extends UserError
    final case class ValidationFailed(error: NonEmptyList[String]) extends UserError
  }

  @Semi(JsCodec) final case class SignUpRequest(
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    password:    Password
  )
  @Semi(JsCodec) final case class SignUpResponse(id: ID[User], sessionID: ID[Session])

  @Semi(JsCodec) final case class SignInResponse(id: ID[Session])

  @Semi(JsCodec) final case class SignOutResponse()

  @Semi(JsCodec) final case class APIUser(
    id:          ID[User],
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    password:    Password,
    permissions: Permissions
  )

  @Semi(JsCodec) final case class UpdateUserRequest()
  @Semi(JsCodec) final case class UpdateUserResponse(id: ID[User])

  @Semi(JsCodec) final case class DeleteUserResponse(id: ID[User])
}
