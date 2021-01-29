package io.branchtalk.users.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.users.api.UserModels._
import sttp.model.StatusCode

object UserModerationAPIs {

  private val prefix = "users" / "moderation"

  private val errorMapping = oneOf[UserError](
    statusMapping[UserError.BadCredentials](StatusCode.Unauthorized, jsonBody[UserError.BadCredentials]),
    statusMapping[UserError.NoPermission](StatusCode.Unauthorized, jsonBody[UserError.NoPermission]),
    statusMapping[UserError.NotFound](StatusCode.NotFound, jsonBody[UserError.NotFound]),
    statusMapping[UserError.ValidationFailed](StatusCode.BadRequest, jsonBody[UserError.ValidationFailed])
  )

  val paginate: AuthedEndpoint[
    (Authentication, Option[PaginationOffset], Option[PaginationLimit]),
    UserError,
    Pagination[APIUser],
    Any
  ] = endpoint
    .name("Fetch Users with User Moderation permission")
    .summary("Paginate User Moderators by name")
    .description("Returns paginated User Moderators")
    .tags(List(UsersTags.domain, UsersTags.userModerators))
    .get
    .in(authHeader)
    .in(prefix)
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIUser]])
    .errorOut(errorMapping)
    .requiringPermissions(_ => RequiredPermissions.anyOf(Permission.ModerateUsers, Permission.Administrate))

  val grantUserModeration: AuthedEndpoint[
    (Authentication, GrantModerationRequest),
    UserError,
    GrantModerationResponse,
    Any
  ] = endpoint
    .name("Grant User Moderation")
    .summary("Grant User Moderation permission")
    .description("Adds User Moderation permission to Users")
    .tags(List(UsersTags.domain, UsersTags.userModerators))
    .post
    .in(authHeader)
    .in(prefix)
    .in(jsonBody[GrantModerationRequest])
    .out(jsonBody[GrantModerationResponse])
    .errorOut(errorMapping)
    .requiringPermissions(_ => RequiredPermissions.one(Permission.Administrate))

  val revokeUserModeration: AuthedEndpoint[
    (Authentication, RevokeModerationRequest),
    UserError,
    RevokeModerationResponse,
    Any
  ] = endpoint
    .name("Revoke User Moderation")
    .summary("Revoke User Moderation permission")
    .description("Removes User Moderation permission from Users")
    .tags(List(UsersTags.domain, UsersTags.userModerators))
    .delete
    .in(authHeader)
    .in(prefix)
    .in(jsonBody[RevokeModerationRequest])
    .out(jsonBody[RevokeModerationResponse])
    .errorOut(errorMapping)
    .requiringPermissions(_ => RequiredPermissions.one(Permission.Administrate))
}
