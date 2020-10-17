package io.branchtalk.users.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.shared.models.ID
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.User
import sttp.tapir._
import sttp.tapir.json.jsoniter._

object UserAPIs {

  private val prefix = "users"

  // TODO: confirm email endpoint
  // TODO: reset password endpoint
  // TODO: list/search users endpoint

  val signUp: Endpoint[SignUpRequest, UserError, SignUpResponse, Nothing] = endpoint.post
    .in(prefix / "sign_up")
    .in(jsonBody[SignUpRequest])
    .out(jsonBody[SignUpResponse])
    .errorOut(jsonBody[UserError])

  // TODO: consider returning user data besides session data
  val signIn: Endpoint[Authentication, UserError, SignInResponse, Nothing] =
    endpoint.post.in(authHeader).in(prefix / "sign_up").out(jsonBody[SignInResponse]).errorOut(jsonBody[UserError])

  val signOut: Endpoint[Authentication, UserError, SignOutResponse, Nothing] =
    endpoint.delete.in(authHeader).in(prefix / "sign_out").out(jsonBody[SignOutResponse]).errorOut(jsonBody[UserError])

  val fetchProfile: Endpoint[ID[User], UserError, APIUser, Nothing] =
    endpoint.get.in(prefix / path[ID[User]]).out(jsonBody[APIUser]).errorOut(jsonBody[UserError])

  val updateProfile: Endpoint[(Authentication, ID[User], UpdateUserRequest), UserError, UpdateUserResponse, Nothing] =
    endpoint.put
      .in(authHeader)
      .in(prefix / path[ID[User]])
      .in(jsonBody[UpdateUserRequest])
      .out(jsonBody[UpdateUserResponse])
      .errorOut(jsonBody[UserError])

  val deleteProfile: Endpoint[(Authentication, ID[User]), UserError, DeleteUserResponse, Nothing] =
    endpoint.delete
      .in(authHeader)
      .in(prefix / path[ID[User]])
      .out(jsonBody[DeleteUserResponse])
      .errorOut(jsonBody[UserError])

  val endpoints: List[Endpoint[_, _, _, _]] = List(signUp, signIn, signOut, fetchProfile, updateProfile, deleteProfile)
}
