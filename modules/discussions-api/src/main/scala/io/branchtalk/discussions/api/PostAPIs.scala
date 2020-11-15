package io.branchtalk.discussions.api

import java.net.URI

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.models.{ ID, Updatable }
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.jsoniter._

object PostAPIs {

  private val prefix = "discussions" / "channels" / path[ID[Channel]].name("channelID") / "posts"

  private val errorMapping = oneOf[PostError](
    statusMapping[PostError.BadCredentials](StatusCode.Unauthorized, jsonBody[PostError.BadCredentials]),
    statusMapping[PostError.NoPermission](StatusCode.Unauthorized, jsonBody[PostError.NoPermission]),
    statusMapping[PostError.NotFound](StatusCode.NotFound, jsonBody[PostError.NotFound]),
    statusMapping[PostError.ValidationFailed](StatusCode.BadRequest, jsonBody[PostError.ValidationFailed])
  )

  val newest: AuthedEndpoint[
    (Option[Authentication], ID[Channel], Option[PaginationOffset], Option[PaginationLimit]),
    PostError,
    Pagination[APIPost],
    Any
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
    .notRequiringPermissions

  val create: AuthedEndpoint[(Authentication, ID[Channel], CreatePostRequest), PostError, CreatePostResponse, Any] =
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
      .notRequiringPermissions

  val read: AuthedEndpoint[(Option[Authentication], ID[Channel], ID[Post]), PostError, APIPost, Any] = endpoint
    .name("Fetch Posts")
    .summary("Fetches specific Post")
    .description("Returns specific Post's data")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / path[ID[Post]].name("postID"))
    .out(jsonBody[APIPost])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val update: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post], UpdatePostRequest),
    PostError,
    UpdatePostResponse,
    Any
  ] = endpoint
    .name("Update Posts")
    .summary("Updates specific Post")
    .description("Schedule specific Post's update, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .put
    .in(authHeader)
    .in(prefix / path[ID[Post]].name("postID"))
    .in(
      jsonBody[UpdatePostRequest].examples(
        List(
          EndpointIO.Example.of(
            UpdatePostRequest(
              newTitle = Updatable.Set(Post.Title("example")),
              newContent = Updatable.Set(Post.Content.Url(Post.URL(URI.create("http://branchtalk.io"))))
            ),
            name = "Set new URL".some,
            summary = "Sets new URL and Title".some
          ),
          EndpointIO.Example.of(
            UpdatePostRequest(
              newTitle = Updatable.Keep,
              newContent = Updatable.Set(Post.Content.Text(Post.Text("Lorem ipsum")))
            ),
            name = "Set new Text".some,
            summary = "Sets new Text".some
          ),
          EndpointIO.Example.of(
            UpdatePostRequest(
              newTitle = Updatable.Keep,
              newContent = Updatable.Keep
            ),
            name = "Keeps both".some,
            summary = "Keeps both Title and Content".some
          )
        )
      )
    )
    .out(jsonBody[UpdatePostResponse])
    .errorOut(errorMapping)
    .requiringPermssions { case (_, channelID, _, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val delete: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post]),
    PostError,
    DeletePostResponse,
    Any
  ] = endpoint
    .name("Delete Posts")
    .summary("Deletes specific Post")
    .description("Schedule specific Post's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .delete
    .in(authHeader)
    .in(prefix / path[ID[Post]].name("postID"))
    .out(jsonBody[DeletePostResponse])
    .errorOut(errorMapping)
    .requiringPermssions { case (_, channelID, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }
}
