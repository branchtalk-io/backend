package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.models.ID
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.jsoniter._

object PostAPIs {

  private val prefix = "discussions" / "channels" / path[ID[Channel]] / "posts"

  private val errorMapping = oneOf[PostError](
    statusMapping[PostError.BadCredentials](StatusCode.Unauthorized, jsonBody[PostError.BadCredentials]),
    statusMapping[PostError.NoPermission](StatusCode.Unauthorized, jsonBody[PostError.NoPermission]),
    statusMapping[PostError.NotFound](StatusCode.NotFound, jsonBody[PostError.NotFound]),
    statusMapping[PostError.ValidationFailed](StatusCode.BadRequest, jsonBody[PostError.ValidationFailed]),
    statusDefaultMapping[PostError](jsonBody[PostError])
  )

  val newest: Endpoint[
    (Option[Authentication], ID[Channel], Option[PaginationOffset], Option[PaginationLimit]),
    PostError,
    Pagination[APIPost],
    Nothing
  ] = endpoint
    .name("Fetch newest Posts")
    .summary("Paginate newest Posts for a Channel")
    .description("Returns paginated Posts for s specific Channel")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(errorMapping)

  val create: Endpoint[(Authentication, ID[Channel], CreatePostRequest), PostError, CreatePostResponse, Nothing] =
    endpoint
      .name("Create Post")
      .summary("Creates Post")
      .description("Schedules Post creation on a specified Channel")
      .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
      .post
      .in(authHeader)
      .in(prefix)
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[CreatePostResponse])
      .errorOut(errorMapping)

  val read: Endpoint[(Option[Authentication], ID[Channel], ID[Post]), PostError, APIPost, Nothing] = endpoint
    .name("Fetch Posts")
    .summary("Fetches specific Post")
    .description("Returns specific Post's data")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / path[ID[Post]])
    .out(jsonBody[APIPost])
    .errorOut(errorMapping)

  val update: Endpoint[
    (Authentication, ID[Channel], ID[Post], UpdatePostRequest, RequiredPermissions),
    PostError,
    UpdatePostResponse,
    Nothing
  ] = endpoint
    .name("Update Posts")
    .summary("Updates specific Post")
    .description("Schedule specific Post's update, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .put
    .in(authHeader)
    .in(prefix / path[ID[Post]])
    .in(jsonBody[UpdatePostRequest])
    .out(jsonBody[UpdatePostResponse])
    .errorOut(errorMapping)
    .requiring { case (_, channelID, _, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val delete: Endpoint[
    (Authentication, ID[Channel], ID[Post], RequiredPermissions),
    PostError,
    DeletePostResponse,
    Nothing
  ] = endpoint
    .name("Delete Posts")
    .summary("Deletes specific Post")
    .description("Schedule specific Post's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .delete
    .in(authHeader)
    .in(prefix / path[ID[Post]])
    .out(jsonBody[DeletePostResponse])
    .errorOut(errorMapping)
    .requiring { case (_, channelID, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }
}
