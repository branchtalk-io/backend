package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.discussions.api.ChannelModels._
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.{ ID, OptionUpdatable, Updatable }
import sttp.model.StatusCode

object ChannelAPIs {

  private val prefix = "discussions" / "channels"

  private val errorMapping = oneOf[ChannelError](
    oneOfVariant[ChannelError.BadCredentials](StatusCode.Unauthorized, jsonBody[ChannelError.BadCredentials]),
    oneOfVariant[ChannelError.NoPermission](StatusCode.Unauthorized, jsonBody[ChannelError.NoPermission]),
    oneOfVariant[ChannelError.NotFound](StatusCode.NotFound, jsonBody[ChannelError.NotFound]),
    oneOfVariant[ChannelError.ValidationFailed](StatusCode.BadRequest, jsonBody[ChannelError.ValidationFailed])
  )

  val paginate: AuthedEndpoint[
    Option[Authentication],
    (Option[PaginationOffset], Option[PaginationLimit]),
    ChannelError,
    Pagination[APIChannel],
    Any
  ] = endpoint
    .name("Fetch Channels")
    .summary("Paginate Channels")
    .description("Returns paginated Channels")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.channels))
    .get
    .securityIn(optAuthHeader)
    .in(prefix)
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIChannel]])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val create: AuthedEndpoint[Authentication, CreateChannelRequest, ChannelError, CreateChannelResponse, Any] =
    endpoint
      .name("Create Channel")
      .summary("Creates Channel")
      .description("Schedules Channel creation")
      .tags(List(DiscussionsTags.domain, DiscussionsTags.channels))
      .post
      .securityIn(authHeader)
      .in(prefix)
      .in(jsonBody[CreateChannelRequest])
      .out(jsonBody[CreateChannelResponse])
      .errorOut(errorMapping)
      .notRequiringPermissions

  val read: AuthedEndpoint[Option[Authentication], ID[Channel], ChannelError, APIChannel, Any] = endpoint
    .name("Fetch Channel")
    .summary("Fetches specific Channel")
    .description("Returns specific Channel's data")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.channels))
    .get
    .securityIn(optAuthHeader)
    .in(prefix / path[ID[Channel]].name("channelID"))
    .out(jsonBody[APIChannel])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val update: AuthedEndpoint[
    Authentication,
    (ID[Channel], UpdateChannelRequest),
    ChannelError,
    UpdateChannelResponse,
    Any
  ] = endpoint
    .name("Update Channel")
    .summary("Updates specific Channel")
    .description("Schedule specific Channel's update, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.channels))
    .put
    .securityIn(authHeader)
    .in(prefix / path[ID[Channel]].name("channelID"))
    .in(
      jsonBody[UpdateChannelRequest].examples(
        List(
          EndpointIO.Example.of(
            UpdateChannelRequest(
              newUrlName = Updatable.Set(Channel.UrlName("example")),
              newName = Updatable.Set(Channel.Name("example")),
              newDescription = OptionUpdatable.Set(Channel.Description("example"))
            ),
            name = "Set new values".some,
            summary = "Sets new URL, Name and Description".some
          ),
          EndpointIO.Example.of(
            UpdateChannelRequest(
              newUrlName = Updatable.Keep,
              newName = Updatable.Keep,
              newDescription = OptionUpdatable.Keep
            ),
            name = "Keep all values".some,
            summary = "Keep all values".some
          ),
          EndpointIO.Example.of(
            UpdateChannelRequest(
              newUrlName = Updatable.Keep,
              newName = Updatable.Keep,
              newDescription = OptionUpdatable.Erase
            ),
            name = "Erase Description".some,
            summary = "Keeps UrlName and Name, erases Description".some
          )
        )
      )
    )
    .out(jsonBody[UpdateChannelResponse])
    .errorOut(errorMapping)
    .requiringPermissions { case (channelID, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val delete: AuthedEndpoint[Authentication, ID[Channel], ChannelError, DeleteChannelResponse, Any] = endpoint
    .name("Delete Channel")
    .summary("Deletes specific Channel")
    .description("Schedule specific Channel's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.channels))
    .delete
    .securityIn(authHeader)
    .in(prefix / path[ID[Channel]].name("channelID"))
    .out(jsonBody[DeleteChannelResponse])
    .errorOut(errorMapping)
    .requiringPermissions(channelID =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    )

  val restore: AuthedEndpoint[Authentication, ID[Channel], ChannelError, RestoreChannelResponse, Any] = endpoint
    .name("Restores Channel")
    .summary("Restores specific Channel")
    .description("Schedule specific Channel's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.channels))
    .post
    .securityIn(authHeader)
    .in(prefix / path[ID[Channel]].name("channelID") / "restore")
    .out(jsonBody[RestoreChannelResponse])
    .errorOut(errorMapping)
    .requiringPermissions(channelID =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    )
}
