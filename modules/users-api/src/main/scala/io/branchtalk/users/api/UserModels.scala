package io.branchtalk.users.api

import cats.data.NonEmptyList
import io.branchtalk.ADT
import io.branchtalk.api._
import io.branchtalk.shared.models.ID
import io.branchtalk.users.model.User
import io.scalaland.catnip.Semi

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object UserModels {

  @Semi(JsCodec) sealed trait UserError extends ADT
  object UserError {

    final case class BadCredentials(msg:     String) extends UserError
    final case class NotFound(msg:           String) extends UserError
    final case class ValidationFailed(error: NonEmptyList[String]) extends UserError
  }

  @Semi(JsCodec) final case class SignUpRequest()
  @Semi(JsCodec) final case class SignUpResponse(id: ID[User], sessionID: SessionID)

  @Semi(JsCodec) final case class SignInResponse(id: SessionID)

  @Semi(JsCodec) final case class SignOutRequest()
  @Semi(JsCodec) final case class SignOutResponse()

  @Semi(JsCodec) final case class APIUser()

  @Semi(JsCodec) final case class UpdateUserRequest()
  @Semi(JsCodec) final case class UpdateUserResponse(id: ID[User])

  @Semi(JsCodec) final case class DeleteUserResponse(id: ID[User])
}
