package io.branchtalk.users.api

import io.branchtalk.api.*
import io.branchtalk.api.AuthenticationSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.shared.model.{ ID, OptionUpdatable, Updatable, branchtalkCharset }
import io.branchtalk.users.api.UserModels.*
import io.branchtalk.users.model.Password.{ Raw => RawPassword }
import io.branchtalk.users.model.{ Session, User }
import sttp.model.StatusCode

object UserAPIs {

  private val prefix = "users"

  private val errorMapping = oneOf[UserError](
    oneOfVariant[UserError.BadCredentials](StatusCode.Unauthorized, jsonBody[UserError.BadCredentials]),
    oneOfVariant[UserError.NoPermission](StatusCode.Forbidden, jsonBody[UserError.NoPermission]),
    oneOfVariant[UserError.NotFound](StatusCode.NotFound, jsonBody[UserError.NotFound]),
    oneOfVariant[UserError.ValidationFailed](StatusCode.BadRequest, jsonBody[UserError.ValidationFailed])
  )

  val paginate: AuthedEndpoint[
    Authentication,
    (Option[Pagination.Offset], Option[Pagination.Limit], Option[String]),
    UserError,
    Pagination[APIUser],
    Any
  ] = endpoint
    .name("Fetch Users")
    .summary("Paginate Users by name")
    .description("Returns paginated Users, optionally filtered by username substring")
    .tags(List(UsersTags.domain, UsersTags.users))
    .get
    .securityIn(authHeader)
    .in(prefix)
    .in(query[Option[Pagination.Offset]]("offset"))
    .in(query[Option[Pagination.Limit]]("limit"))
    .in(
      query[Option[String]]("nameContains")
        .description("Filter users whose username contains this string (case-insensitive)")
    )
    .out(jsonBody[Pagination[APIUser]])
    .errorOut(errorMapping)
    .requiringPermissions(_ => RequiredPermissions.one(Permission.ModerateUsers))

  val newest: AuthedEndpoint[
    Authentication,
    (Option[Pagination.Offset], Option[Pagination.Limit]),
    UserError,
    Pagination[APIUser],
    Any
  ] = endpoint
    .name("Paginate newest Users")
    .summary("Paginate newest Users")
    .description("Returns paginated newest Users")
    .tags(List(UsersTags.domain, UsersTags.users))
    .get
    .securityIn(authHeader)
    .in(prefix / "newest")
    .in(query[Option[Pagination.Offset]]("offset"))
    .in(query[Option[Pagination.Limit]]("limit"))
    .out(jsonBody[Pagination[APIUser]])
    .errorOut(errorMapping)
    .requiringPermissions(_ => RequiredPermissions.one(Permission.ModerateUsers))

  val sessions: AuthedEndpoint[
    Authentication,
    (Option[Pagination.Offset], Option[Pagination.Limit]),
    UserError,
    Pagination[APISession],
    Any
  ] = endpoint
    .name("Paginate Session")
    .summary("Paginate Sessions")
    .description("Returns paginated Sessions by closest expiration date")
    .tags(List(UsersTags.domain, UsersTags.sessions))
    .get
    .securityIn(authHeader)
    .in(prefix / "sessions")
    .in(query[Option[Pagination.Offset]]("offset"))
    .in(query[Option[Pagination.Limit]]("limit"))
    .out(jsonBody[Pagination[APISession]])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val signUp: Endpoint[Unit, (SignUpRequest, Option[String], Option[String]), UserError, SignUpResponse, Any] = endpoint
    .name("Sign up")
    .summary("Allows creation of User's account")
    .description("Schedules User creation and returns future User's ID as well as future Session's handler")
    .tags(List(UsersTags.domain, UsersTags.users, UsersTags.sessions))
    .post
    .in(prefix)
    .in(jsonBody[SignUpRequest])
    .in(header[Option[String]]("User-Agent"))
    .in(header[Option[String]]("X-Forwarded-For"))
    .out(jsonBody[SignUpResponse])
    .errorOut(errorMapping)

  val signIn: AuthedEndpoint[Authentication, (Option[String], Option[String]), UserError, SignInResponse, Any] =
    endpoint
      .name("Sign in")
      .summary("Allows logging into existing User's account")
      .description("Returns Session's handler")
      .tags(List(UsersTags.domain, UsersTags.sessions))
      .post
      .securityIn(authHeader)
      .in(prefix / "session")
      .in(header[Option[String]]("User-Agent"))
      .in(header[Option[String]]("X-Forwarded-For"))
      .out(jsonBody[SignInResponse])
      .errorOut(errorMapping)
      .notRequiringPermissions

  val signOut: AuthedEndpoint[Authentication, Unit, UserError, SignOutResponse, Any] = endpoint
    .name("Sign out")
    .summary("Destroys specific User's session")
    .description("Make the Session ID immediately invalid")
    .tags(List(UsersTags.domain, UsersTags.sessions))
    .delete
    .securityIn(authHeader)
    .in(prefix / "session")
    .out(jsonBody[SignOutResponse])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val fetchProfile: AuthedEndpoint[Option[Authentication], ID[User], UserError, APIUser, Any] = endpoint
    .name("Fetch profile")
    .summary("Fetches specific User's profile")
    .description("Returns User's profile")
    .tags(List(UsersTags.domain, UsersTags.users))
    .get
    .securityIn(optAuthHeader)
    .in(prefix / path[ID[User]].name("userID"))
    .out(jsonBody[APIUser])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val updateProfile: AuthedEndpoint[Authentication, (ID[User], UpdateUserRequest), UserError, UpdateUserResponse, Any] =
    endpoint
      .name("Update profile")
      .summary("Updates specific User's profile")
      .description("Schedules specific User's profile update, requires ownership or moderator status")
      .tags(List(UsersTags.domain, UsersTags.users))
      .put
      .securityIn(authHeader)
      .in(prefix / path[ID[User]].name("userID"))
      .in(
        jsonBody[UpdateUserRequest].examples(
          List(
            EndpointIO.Example.of(
              UpdateUserRequest(
                newUsername = Updatable.Set(User.Name("example")),
                newDescription = OptionUpdatable.Set(User.Description("example")),
                newPassword = Updatable.Set(RawPassword.unsafeMake("example".getBytes(branchtalkCharset)))
              ),
              name = "Set all".some,
              summary = "Assigns new value to all fields".some
            ),
            EndpointIO.Example.of(
              UpdateUserRequest(
                newUsername = Updatable.Keep,
                newDescription = OptionUpdatable.Keep,
                newPassword = Updatable.Keep
              ),
              name = "Keep all".some,
              summary = "Keeps old value for all fields".some
            ),
            EndpointIO.Example.of(
              UpdateUserRequest(
                newUsername = Updatable.Keep,
                newDescription = OptionUpdatable.Erase,
                newPassword = Updatable.Keep
              ),
              name = "Erase description".some,
              summary = "Erases optional value".some
            )
          )
        )
      )
      .out(jsonBody[UpdateUserResponse])
      .errorOut(errorMapping)
      .requiringPermissions(_ => RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateUsers))

  val deleteProfile: AuthedEndpoint[Authentication, ID[User], UserError, DeleteUserResponse, Any] = endpoint
    .name("Delete profile")
    .summary("Deletes specific User's profile")
    .description("Schedules specific User's profile deletion, requires ownership or moderator status, cannot be undone")
    .tags(List(UsersTags.domain, UsersTags.users))
    .delete
    .securityIn(authHeader)
    .in(prefix / path[ID[User]].name("userID"))
    .out(jsonBody[DeleteUserResponse])
    .errorOut(errorMapping)
    .requiringPermissions(_ => RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateUsers))

  // Issue #7: Remote sign-out - delete a specific session by ID
  val deleteSession: AuthedEndpoint[Authentication, ID[Session], UserError, DeleteSessionResponse, Any] = endpoint
    .name("Delete session")
    .summary("Deletes a specific Session by ID (remote sign-out)")
    .description("Deletes one of the caller's own sessions by ID, verifying ownership")
    .tags(List(UsersTags.domain, UsersTags.sessions))
    .delete
    .securityIn(authHeader)
    .in(prefix / "sessions" / path[ID[Session]].name("sessionID"))
    .out(jsonBody[DeleteSessionResponse])
    .errorOut(errorMapping)
    .notRequiringPermissions

  // Issue #8: Request email update
  val requestEmailUpdate: AuthedEndpoint[Authentication,
                                         (ID[User], RequestEmailUpdateRequest),
                                         UserError,
                                         RequestEmailUpdateResponse,
                                         Any
  ] =
    endpoint
      .name("Request email update")
      .summary("Requests an email address change")
      .description(
        "Sets a pending email and generates a confirmation token. The actual email is promoted when the token is confirmed."
      )
      .tags(List(UsersTags.domain, UsersTags.users))
      .post
      .securityIn(authHeader)
      .in(prefix / path[ID[User]].name("userID") / "email")
      .in(jsonBody[RequestEmailUpdateRequest])
      .out(jsonBody[RequestEmailUpdateResponse])
      .errorOut(errorMapping)
      .requiringPermissions(_ => RequiredPermissions.one(Permission.IsOwner))

  // Issue #8: Confirm email update
  val confirmEmail: AuthedEndpoint[Authentication,
                                   (ID[User], ConfirmEmailRequest),
                                   UserError,
                                   ConfirmEmailResponse,
                                   Any
  ] =
    endpoint
      .name("Confirm email")
      .summary("Confirms an email address change")
      .description("Promotes the pending email to the user's primary email when the correct token is provided.")
      .tags(List(UsersTags.domain, UsersTags.users))
      .post
      .securityIn(authHeader)
      .in(prefix / path[ID[User]].name("userID") / "email" / "confirm")
      .in(jsonBody[ConfirmEmailRequest])
      .out(jsonBody[ConfirmEmailResponse])
      .errorOut(errorMapping)
      .requiringPermissions(_ => RequiredPermissions.one(Permission.IsOwner))
}
