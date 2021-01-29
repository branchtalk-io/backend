package io.branchtalk.users.api

import java.time.OffsetDateTime

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.ADT
import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.shared.model.{ ID, OptionUpdatable, Updatable }
import io.branchtalk.users.model.SessionProperties.Usage.Type
import io.branchtalk.users.model._
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._
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
  implicit val passwordRawCodec: JsCodec[Password.Raw] =
    summonCodec[String](JsonCodecMaker.make)
      .map[Array[Byte]](_.getBytes)(new String(_)) // I wanted to avoid that but the result is ugly :/
      .refine[NonEmpty] // I'll try to revisit that someday and e.g. use Base64 here?
      .asNewtype[Password.Raw]
  implicit val permissionsCodec: JsCodec[Permissions] =
    summonCodec[Set[Permission]](JsonCodecMaker.make).asNewtype[Permissions]
  implicit val sessionExpirationCodec: JsCodec[Session.ExpirationTime] =
    summonCodec[OffsetDateTime](JsonCodecMaker.make).asNewtype[Session.ExpirationTime]
  implicit val banReasonCodec: JsCodec[Ban.Reason] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Ban.Reason]

  // properties schemas
  implicit val userEmailSchema: Schema[User.Email] =
    summonSchema[String Refined MatchesRegex["(.+)@(.+)"]].asNewtype[User.Email]
  implicit val usernameSchema: Schema[User.Name] =
    summonSchema[String Refined NonEmpty].asNewtype[User.Name]
  implicit val userDescriptionSchema: Schema[User.Description] =
    summonSchema[String].asNewtype[User.Description]
  implicit val passwordRawSchema: Schema[Password.Raw] =
    summonSchema[String].contramap[Array[Byte] Refined NonEmpty](_.value.pipe(new String(_))).asNewtype[Password.Raw]
  implicit val permissionSchema: JsSchema[Permission] =
    Schema.derived[Permission]
  implicit val permissionsSchema: Schema[Permissions] =
    summonSchema[Set[Permission]].asNewtype[Permissions]
  implicit val sessionExpirationSchema: Schema[Session.ExpirationTime] =
    summonSchema[OffsetDateTime].asNewtype[Session.ExpirationTime]
  implicit val banReasonSchema: Schema[Ban.Reason] =
    summonSchema[NonEmptyString].asNewtype[Ban.Reason]

  @Semi(JsCodec, JsSchema) sealed trait UserError extends ADT
  object UserError {

    @Semi(JsCodec, JsSchema) final case class BadCredentials(msg: String) extends UserError
    @Semi(JsCodec, JsSchema) final case class NoPermission(msg: String) extends UserError
    @Semi(JsCodec, JsSchema) final case class NotFound(msg: String) extends UserError
    @Semi(JsCodec, JsSchema) final case class ValidationFailed(error: NonEmptyList[String]) extends UserError
  }

  @Semi(JsCodec, JsSchema) final case class APISession(
    id:          ID[Session],
    userID:      ID[User],
    sessionType: APISession.SessionType,
    expiresAt:   Session.ExpirationTime
  )
  object APISession {

    @Semi(JsCodec, JsSchema) sealed trait SessionType extends ADT
    object SessionType {
      case object UserSession extends SessionType
      case object OAuth extends SessionType
    }

    def fromDomain(session: Session): APISession = {
      val Session.Usage.Tupled(domainSessionType, _) = session.data.usage
      val sessionType = domainSessionType match {
        case Type.UserSession => SessionType.UserSession
        case Type.OAuth       => SessionType.OAuth
      }
      session.data
        .into[APISession]
        .withFieldConst(_.id, session.id)
        .withFieldConst(_.sessionType, sessionType)
        .transform
    }
  }

  @Semi(JsCodec, JsSchema) final case class SignUpRequest(
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    password:    Password.Raw
  )
  @Semi(JsCodec, JsSchema) final case class SignUpResponse(
    userID:    ID[User],
    sessionID: ID[Session]
  )

  @Semi(JsCodec, JsSchema) final case class SignInResponse(
    userID:    ID[User],
    sessionID: ID[Session],
    expiresAt: Session.ExpirationTime
  )

  @Semi(JsCodec, JsSchema) final case class SignOutResponse(
    userID:    ID[User],
    sessionID: Option[ID[Session]] // in case user wasn't using sessionID
  )

  @Semi(JsCodec, JsSchema) final case class APIUser(
    id:          ID[User],
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    permissions: Permissions
  )
  object APIUser {

    def fromDomain(user: User): APIUser = user.data.into[APIUser].withFieldConst(_.id, user.id).transform
  }

  @Semi(JsCodec, JsSchema) final case class UpdateUserRequest(
    newUsername:    Updatable[User.Name],
    newDescription: OptionUpdatable[User.Description],
    newPassword:    Updatable[Password.Raw]
  )
  @Semi(JsCodec, JsSchema) final case class UpdateUserResponse(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class DeleteUserResponse(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class GrantModerationRequest(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class GrantModerationResponse(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class RevokeModerationRequest(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class RevokeModerationResponse(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class BansResponse(bannedIDs: List[ID[User]])

  @Semi(JsCodec, JsSchema) final case class BanOrderRequest(id: ID[User], reason: Ban.Reason)

  @Semi(JsCodec, JsSchema) final case class BanOrderResponse(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class BanLiftRequest(id: ID[User])

  @Semi(JsCodec, JsSchema) final case class BanLiftResponse(id: ID[User])
}
