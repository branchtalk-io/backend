package io.branchtalk.users.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.shared.model.ID
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.Channel
import sttp.model.StatusCode

object ChannelModerationAPIs {

  private val prefix = "discussions" / "channels" / path[ID[Channel]].name("channelID") / "moderation"

  private val errorMapping = oneOf[UserError](
    oneOfVariant[UserError.BadCredentials](StatusCode.Unauthorized, jsonBody[UserError.BadCredentials]),
    oneOfVariant[UserError.NoPermission](StatusCode.Unauthorized, jsonBody[UserError.NoPermission]),
    oneOfVariant[UserError.NotFound](StatusCode.NotFound, jsonBody[UserError.NotFound]),
    oneOfVariant[UserError.ValidationFailed](StatusCode.BadRequest, jsonBody[UserError.ValidationFailed])
  )

  val paginate: AuthedEndpoint[
    Authentication,
    (ID[Channel], Option[PaginationOffset], Option[PaginationLimit]),
    UserError,
    Pagination[APIUser],
    Any
  ] = endpoint
    .name("Fetch Users with Channel Moderation permission")
    .summary("Paginate Channel Moderators by name")
    .description("Returns paginated Channel Moderators")
    .tags(List(UsersTags.domain, UsersTags.channelModerators))
    .get
    .securityIn(authHeader)
    .in(prefix)
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIUser]])
    .errorOut(errorMapping)
    .requiringPermissions { case (channelID, _, _) =>
      RequiredPermissions.anyOf(Permission.ModerateChannel(ChannelID(channelID.uuid)), Permission.Administrate)
    }

  val grantChannelModeration: AuthedEndpoint[
    Authentication,
    (ID[Channel], GrantModerationRequest),
    UserError,
    GrantModerationResponse,
    Any
  ] = endpoint
    .name("Grant Channel Moderation")
    .summary("Grant Channel Moderation permission")
    .description("Adds Channel Moderation permission to Users")
    .tags(List(UsersTags.domain, UsersTags.channelModerators))
    .post
    .securityIn(authHeader)
    .in(prefix)
    .in(jsonBody[GrantModerationRequest])
    .out(jsonBody[GrantModerationResponse])
    .errorOut(errorMapping)
    .requiringPermissions { case (channelID, _) =>
      RequiredPermissions.anyOf(Permission.ModerateChannel(ChannelID(channelID.uuid)), Permission.Administrate)
    }

  val revokeChannelModeration: AuthedEndpoint[
    Authentication,
    (ID[Channel], RevokeModerationRequest),
    UserError,
    RevokeModerationResponse,
    Any
  ] = endpoint
    .name("Revoke Channel Moderation")
    .summary("Revoke Channel Moderation permission")
    .description("Removes Channel Moderation permission from Users")
    .tags(List(UsersTags.domain, UsersTags.channelModerators))
    .delete
    .securityIn(authHeader)
    .in(prefix)
    .in(jsonBody[RevokeModerationRequest])
    .out(jsonBody[RevokeModerationResponse])
    .errorOut(errorMapping)
    .requiringPermissions { case (channelID, _) =>
      RequiredPermissions.anyOf(Permission.ModerateChannel(ChannelID(channelID.uuid)), Permission.Administrate)
    }
}
