package io.branchtalk.discussions.api

import java.net.URI

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.model.{ ID, Updatable }
import sttp.model.StatusCode

object PostAPIs {

  private val prefix = "discussions" / "channels" / path[ID[Channel]].name("channelID") / "posts"

  private[api] val errorMapping = oneOf[PostError](
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
    .description("Returns paginated Posts for a specific Channel")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val hottest: AuthedEndpoint[(Option[Authentication], ID[Channel]), PostError, Pagination[APIPost], Any] = endpoint
    .name("Fetch hottest Posts")
    .summary("Paginate hottest Posts for a Channel")
    .description("Returns paginated Posts for a specific Channel")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / "hottest")
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val controversial: AuthedEndpoint[(Option[Authentication], ID[Channel]), PostError, Pagination[APIPost], Any] =
    endpoint
      .name("Fetch controversial Posts")
      .summary("Paginate controversial Posts for a Channel")
      .description("Returns paginated Posts for a specific Channel")
      .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
      .get
      .in(optAuthHeader)
      .in(prefix / "controversial")
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
    .name("Fetch Post")
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
    .name("Update Post")
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
            name = "Keep both".some,
            summary = "Keeps both Title and Content".some
          )
        )
      )
    )
    .out(jsonBody[UpdatePostResponse])
    .errorOut(errorMapping)
    .requiringPermissions { case (_, channelID, _, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val delete: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post]),
    PostError,
    DeletePostResponse,
    Any
  ] = endpoint
    .name("Delete Post")
    .summary("Deletes specific Post")
    .description("Schedule specific Post's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .delete
    .in(authHeader)
    .in(prefix / path[ID[Post]].name("postID"))
    .out(jsonBody[DeletePostResponse])
    .errorOut(errorMapping)
    .requiringPermissions { case (_, channelID, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val restore: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post]),
    PostError,
    RestorePostResponse,
    Any
  ] = endpoint
    .name("Restores Post")
    .summary("Restores specific Post")
    .description("Schedule specific Post's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .post
    .in(authHeader)
    .in(prefix / path[ID[Post]].name("postID") / "restore")
    .out(jsonBody[RestorePostResponse])
    .errorOut(errorMapping)
    .requiringPermissions { case (_, channelID, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val upvote: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post]),
    PostError,
    Unit,
    Any
  ] = endpoint
    .name("Upvotes Post")
    .summary("Upvotes specific Post")
    .description("Schedule specific Post's upvote")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .put
    .in(authHeader)
    .in(prefix / path[ID[Post]].name("postID") / "upvote")
    .errorOut(errorMapping)
    .notRequiringPermissions

  val downvote: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post]),
    PostError,
    Unit,
    Any
  ] = endpoint
    .name("Downvotes Post")
    .summary("Downvotes specific Post")
    .description("Schedule specific Post's downvote")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .put
    .in(authHeader)
    .in(prefix / path[ID[Post]].name("postID") / "upvote")
    .errorOut(errorMapping)
    .notRequiringPermissions

  val revokeVote: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post]),
    PostError,
    Unit,
    Any
  ] = endpoint
    .name("Revokes Post vote")
    .summary("Revokes vote for a specific Post")
    .description("Schedule specific Post's vote's revoke")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .put
    .in(authHeader)
    .in(prefix / path[ID[Post]].name("postID") / "revoke-vote")
    .errorOut(errorMapping)
    .notRequiringPermissions
}
