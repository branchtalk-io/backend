package io.branchtalk.users.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.shared.model.ID
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.Channel
import sttp.model.StatusCode

object ChannelBanAPIs {

  private val prefix = "discussions" / "channels" / path[ID[Channel]].name("channelID") / "bans"

  private val errorMapping = oneOf[UserError](
    oneOfVariant[UserError.BadCredentials](StatusCode.Unauthorized, jsonBody[UserError.BadCredentials]),
    oneOfVariant[UserError.NoPermission](StatusCode.Unauthorized, jsonBody[UserError.NoPermission]),
    oneOfVariant[UserError.NotFound](StatusCode.NotFound, jsonBody[UserError.NotFound]),
    oneOfVariant[UserError.ValidationFailed](StatusCode.BadRequest, jsonBody[UserError.ValidationFailed])
  )

  // consider turning it into pagination
  val list: AuthedEndpoint[Authentication, ID[Channel], UserError, BansResponse, Any] = endpoint
    .name("Fetch banned from Channel")
    .summary("Fetches list of all Users banned from the Channel")
    .description("IDs of all banned Users")
    .tags(List(UsersTags.domain, UsersTags.channelModerators))
    .get
    .securityIn(authHeader)
    .in(prefix)
    .out(jsonBody[BansResponse])
    .errorOut(errorMapping)
    .requiringPermissions(channelID =>
      RequiredPermissions.anyOf(Permission.ModerateChannel(ChannelID(channelID.uuid)), Permission.Administrate)
    )

  val orderBan: AuthedEndpoint[Authentication, (ID[Channel], BanOrderRequest), UserError, BanOrderResponse, Any] =
    endpoint
      .name("Ban User from Channel")
      .summary("Orders User's Ban from Channel")
      .description("Adds User to the list of banned from posting on Channel")
      .tags(List(UsersTags.domain, UsersTags.channelModerators))
      .post
      .securityIn(authHeader)
      .in(prefix)
      .in(jsonBody[BanOrderRequest])
      .out(jsonBody[BanOrderResponse])
      .errorOut(errorMapping)
      .requiringPermissions { case (channelID, _) =>
        RequiredPermissions.anyOf(Permission.ModerateChannel(ChannelID(channelID.uuid)), Permission.Administrate)
      }

  val liftBan: AuthedEndpoint[Authentication, (ID[Channel], BanLiftRequest), UserError, BanLiftResponse, Any] =
    endpoint
      .name("Unban User from Channel")
      .summary("Lifts User's Ban from Channel")
      .description("Removes User from the list of banned from posting on Channel")
      .tags(List(UsersTags.domain, UsersTags.channelModerators))
      .delete
      .securityIn(authHeader)
      .in(prefix)
      .in(jsonBody[BanLiftRequest])
      .out(jsonBody[BanLiftResponse])
      .errorOut(errorMapping)
      .requiringPermissions { case (channelID, _) =>
        RequiredPermissions.anyOf(Permission.ModerateChannel(ChannelID(channelID.uuid)), Permission.Administrate)
      }
}
