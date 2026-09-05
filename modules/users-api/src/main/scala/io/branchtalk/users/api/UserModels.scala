package io.branchtalk.users.api

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.shared.model.*
import io.branchtalk.users.model.*
import io.scalaland.chimney.dsl.*

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object UserModels {

  // properties codecs
  // Plain neotype newtypes (User.Email/Name/Description, Session.ExpirationTime, Ban.Reason, Permissions) are handled
  // uniformly by the Kindlings IsValueType macro-extension (neotype-kindlings) - no per-companion codec needed.
  // Only Password.Raw needs an explicit codec because it uses a custom String<->Array[Byte] representation.
  // I wanted to avoid that but the result is ugly :/
  // I'll try to revisit that someday and e.g. use Base64 here?
  given JsCodec[Password.Raw] =
    DefaultJsCodec.derived[String].map[Array[Byte]](_.getBytes)(new String(_)).asNewtypeCodec

  // properties schemas
  // Permission (ADT) needs an explicit Kindlings schema because it's a domain enum without a `derives` clause; from it
  // the Permissions (Set newtype) schema follows via the generic newtype schema given. Password.Raw needs an explicit
  // schema because its JSON representation is a custom String (not Array[Byte]).
  given JsSchema[Permission] = JsSchema.derived
  given JsSchema[Password.Raw] =
    JsSchema.schemaForString.map[Array[Byte]](_.getBytes.some)(new String(_)).asNewtypeSchema[Password.Raw]

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
    expiresAt:   Session.ExpirationTime,
    ipAddress:   Option[Session.IpAddress],
    userAgent:   Option[Session.UserAgent]
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

  final case class DeleteSessionResponse(
    sessionID: ID[Session]
  ) derives DefaultJsCodec,
        JsSchema

  // Issue #8 - Email confirmation lifecycle
  final case class RequestEmailUpdateRequest(
    newEmail: User.Email
  ) derives DefaultJsCodec,
        JsSchema

  final case class RequestEmailUpdateResponse(
    id:    ID[User],
    token: User.EmailConfirmationToken
  ) derives DefaultJsCodec,
        JsSchema

  final case class ConfirmEmailRequest(
    token: User.EmailConfirmationToken
  ) derives DefaultJsCodec,
        JsSchema

  final case class ConfirmEmailResponse(
    id: ID[User]
  ) derives DefaultJsCodec,
        JsSchema
}
