package io.branchtalk.users.api

import java.time.OffsetDateTime
import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.shared.model.*
import io.branchtalk.users.model.*
import io.scalaland.chimney.dsl.*
import sttp.tapir.Schema

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object UserModels {

  // properties codecs
  given JsCodec[User.Email]       = newtypeCodec
  given JsCodec[User.Name]        = newtypeCodec
  given JsCodec[User.Description] = newtypeCodec
  // I wanted to avoid that but the result is ugly :/
  // I'll try to revisit that someday and e.g. use Base64 here?
  given JsCodec[Password.Raw] =
    DefaultJsCodec.derived[String].map[Array[Byte]](_.getBytes)(new String(_)).asNewtypeCodec
  given JsCodec[Permissions]            = newtypeCodec
  given JsCodec[Session.ExpirationTime] = newtypeCodec
  given JsCodec[Ban.Reason]             = newtypeCodec

  // properties schemas
  given JsSchema[Permission]  = JsSchema.derived
  given JsSchema[Permissions] = summonSchema[List[Permission]].as[Set[Permission]].asNewtypeSchema[Permissions]
  given JsSchema[Password.Raw] =
    summonSchema[String].map[Array[Byte]](_.getBytes.some)(new String(_)).asNewtypeSchema[Password.Raw]

  sealed trait UserError derives DefaultJsCodec, JsSchema
  object UserError {

    final case class BadCredentials(msg: String) extends UserError derives DefaultJsCodec, JsSchema
    final case class NoPermission(msg: String) extends UserError derives DefaultJsCodec, JsSchema
    final case class NotFound(msg: String) extends UserError derives DefaultJsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends UserError derives DefaultJsCodec, JsSchema
  }

  final case class APISession(
    id:          ID[Session],
    userID:      ID[User],
    sessionType: APISession.SessionType,
    expiresAt:   Session.ExpirationTime
  ) derives DefaultJsCodec,
        JsSchema
  object APISession {

    sealed trait SessionType derives DefaultJsCodec, JsSchema
    object SessionType {
      case object UserSession extends SessionType
      case object OAuth extends SessionType
    }

    def fromDomain(session: Session): APISession = {
      val Session.Usage.Tupled(domainSessionType, _) = session.data.usage
      val sessionType = domainSessionType match {
        case Session.Usage.Type.UserSession => SessionType.UserSession
        case Session.Usage.Type.OAuth       => SessionType.OAuth
      }
      session.data
        .into[APISession]
        .withFieldConst(_.id, session.id)
        .withFieldConst(_.sessionType, sessionType)
        .transform
    }
  }

  final case class SignUpRequest(
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    password:    Password.Raw
  ) derives DefaultJsCodec,
        JsSchema
  final case class SignUpResponse(
    userID:    ID[User],
    sessionID: ID[Session]
  ) derives DefaultJsCodec,
        JsSchema

  final case class SignInResponse(
    userID:    ID[User],
    sessionID: ID[Session],
    expiresAt: Session.ExpirationTime
  ) derives DefaultJsCodec,
        JsSchema

  final case class SignOutResponse(
    userID:    ID[User],
    sessionID: Option[ID[Session]] // in case user wasn't using sessionID
  ) derives DefaultJsCodec,
        JsSchema

  final case class APIUser(
    id:          ID[User],
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    permissions: Permissions
  ) derives DefaultJsCodec,
        JsSchema
  object APIUser {

    def fromDomain(user: User): APIUser = user.data.into[APIUser].withFieldConst(_.id, user.id).transform
  }

  final case class UpdateUserRequest(
    newUsername:    Updatable[User.Name],
    newDescription: OptionUpdatable[User.Description],
    newPassword:    Updatable[Password.Raw]
  ) derives DefaultJsCodec,
        JsSchema
  final case class UpdateUserResponse(id: ID[User]) derives DefaultJsCodec, JsSchema

  final case class DeleteUserResponse(id: ID[User]) derives DefaultJsCodec, JsSchema

  final case class GrantModerationRequest(id: ID[User]) derives DefaultJsCodec, JsSchema
  final case class GrantModerationResponse(id: ID[User]) derives DefaultJsCodec, JsSchema

  final case class RevokeModerationRequest(id: ID[User]) derives DefaultJsCodec, JsSchema
  final case class RevokeModerationResponse(id: ID[User]) derives DefaultJsCodec, JsSchema

  final case class BansResponse(bannedIDs: List[ID[User]]) derives DefaultJsCodec, JsSchema

  final case class BanOrderRequest(id: ID[User], reason: Ban.Reason) derives DefaultJsCodec, JsSchema
  final case class BanOrderResponse(id: ID[User]) derives DefaultJsCodec, JsSchema

  final case class BanLiftRequest(id: ID[User]) derives DefaultJsCodec, JsSchema
  final case class BanLiftResponse(id: ID[User]) derives DefaultJsCodec, JsSchema
}
