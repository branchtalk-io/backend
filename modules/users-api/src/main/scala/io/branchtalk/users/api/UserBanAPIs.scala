package io.branchtalk.users.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.users.api.UserModels._
import sttp.model.StatusCode

object UserBanAPIs {

  private val prefix = "users" / "bans"

  private val errorMapping = oneOf[UserError](
    oneOfVariant[UserError.BadCredentials](StatusCode.Unauthorized, jsonBody[UserError.BadCredentials]),
    oneOfVariant[UserError.NoPermission](StatusCode.Unauthorized, jsonBody[UserError.NoPermission]),
    oneOfVariant[UserError.NotFound](StatusCode.NotFound, jsonBody[UserError.NotFound]),
    oneOfVariant[UserError.ValidationFailed](StatusCode.BadRequest, jsonBody[UserError.ValidationFailed])
  )

  // consider turning it into pagination
  val list: AuthedEndpoint[Authentication, Unit, UserError, BansResponse, Any] = endpoint
    .name("Fetch banned globally")
    .summary("Fetches list of all Users banned globally")
    .description("IDs of all banned Users")
    .tags(List(UsersTags.domain, UsersTags.channelModerators))
    .get
    .securityIn(authHeader)
    .in(prefix)
    .out(jsonBody[BansResponse])
    .errorOut(errorMapping)
    .requiringPermissions(_ => RequiredPermissions.one(Permission.Administrate))

  val orderBan: AuthedEndpoint[Authentication, BanOrderRequest, UserError, BanOrderResponse, Any] =
    endpoint
      .name("Ban User globally")
      .summary("Orders User's global Ban")
      .description("Adds User to the list of globally banned")
      .tags(List(UsersTags.domain, UsersTags.channelModerators))
      .post
      .securityIn(authHeader)
      .in(prefix)
      .in(jsonBody[BanOrderRequest])
      .out(jsonBody[BanOrderResponse])
      .errorOut(errorMapping)
      .requiringPermissions(_ => RequiredPermissions.one(Permission.Administrate))

  val liftBan: AuthedEndpoint[Authentication, BanLiftRequest, UserError, BanLiftResponse, Any] =
    endpoint
      .name("Unban User globally")
      .summary("Lifts User's global Ban")
      .description("Removes User from the list of globally banned")
      .tags(List(UsersTags.domain, UsersTags.channelModerators))
      .delete
      .securityIn(authHeader)
      .in(prefix)
      .in(jsonBody[BanLiftRequest])
      .out(jsonBody[BanLiftResponse])
      .errorOut(errorMapping)
      .requiringPermissions(_ => RequiredPermissions.one(Permission.Administrate))
}
