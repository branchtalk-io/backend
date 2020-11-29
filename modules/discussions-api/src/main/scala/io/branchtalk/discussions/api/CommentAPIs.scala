package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.discussions.api.CommentModels._
import io.branchtalk.discussions.model.{ Channel, Comment, Post }
import io.branchtalk.shared.model.{ ID, Updatable }
import sttp.model.StatusCode
import sttp.tapir._

object CommentAPIs {

  private val prefix =
    "discussions" / "channels" / path[ID[Channel]].name("channelID") / "posts" / path[ID[Post]]
      .name("postID") / "comments"

  private val errorMapping = oneOf[CommentError](
    statusMapping[CommentError.BadCredentials](StatusCode.Unauthorized, jsonBody[CommentError.BadCredentials]),
    statusMapping[CommentError.NoPermission](StatusCode.Unauthorized, jsonBody[CommentError.NoPermission]),
    statusMapping[CommentError.NotFound](StatusCode.NotFound, jsonBody[CommentError.NotFound]),
    statusMapping[CommentError.ValidationFailed](StatusCode.BadRequest, jsonBody[CommentError.ValidationFailed])
  )

  val newest: AuthedEndpoint[
    (
      Option[Authentication],
      ID[Channel],
      ID[Post],
      Option[PaginationOffset],
      Option[PaginationLimit],
      Option[ID[Comment]]
    ),
    CommentError,
    Pagination[APIComment],
    Any
  ] = endpoint
    .name("Fetch newest Comments")
    .summary("Paginate newest Comments for a Post")
    .description("Returns paginated Comments for a specific Post (and maybe even replies to a specific Comment")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.comments))
    .get
    .in(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .in(query[Option[ID[Comment]]]("reply-to"))
    .out(jsonBody[Pagination[APIComment]])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val create: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post], CreateCommentRequest),
    CommentError,
    CreateCommentResponse,
    Any
  ] =
    endpoint
      .name("Create Comment")
      .summary("Creates Comment")
      .description("Schedules Comment creation for a specified Post")
      .tags(List(DiscussionsTags.domain, DiscussionsTags.comments))
      .post
      .in(authHeader)
      .in(prefix)
      .in(jsonBody[CreateCommentRequest])
      .out(jsonBody[CreateCommentResponse])
      .errorOut(errorMapping)
      .notRequiringPermissions

  val read: AuthedEndpoint[
    (Option[Authentication], ID[Channel], ID[Post], ID[Comment]),
    CommentError,
    APIComment,
    Any
  ] = endpoint
    .name("Fetch Comment")
    .summary("Fetches specific Comment")
    .description("Returns specific Comment's data")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.comments))
    .get
    .in(optAuthHeader)
    .in(prefix / path[ID[Comment]].name("commentID"))
    .out(jsonBody[APIComment])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val update: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post], ID[Comment], UpdateCommentRequest),
    CommentError,
    UpdateCommentResponse,
    Any
  ] = endpoint
    .name("Update Comment")
    .summary("Updates specific Comment")
    .description("Schedule specific Comment's update, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.comments))
    .put
    .in(authHeader)
    .in(prefix / path[ID[Comment]].name("commentID"))
    .in(
      jsonBody[UpdateCommentRequest].examples(
        List(
          EndpointIO.Example.of(
            UpdateCommentRequest(
              newContent = Updatable.Set(Comment.Content("example"))
            ),
            name = "Set new content".some,
            summary = "Sets new Content".some
          ),
          EndpointIO.Example.of(
            UpdateCommentRequest(
              newContent = Updatable.Keep
            ),
            name = "Keep old Content".some,
            summary = "Keeps old Content".some
          )
        )
      )
    )
    .out(jsonBody[UpdateCommentResponse])
    .errorOut(errorMapping)
    .requiringPermssions { case (_, channelID, _, _, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val delete: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post], ID[Comment]),
    CommentError,
    DeleteCommentResponse,
    Any
  ] = endpoint
    .name("Delete Comment")
    .summary("Deletes specific Comment")
    .description("Schedule specific Comment's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.comments))
    .delete
    .in(authHeader)
    .in(prefix / path[ID[Comment]].name("commentID"))
    .out(jsonBody[DeleteCommentResponse])
    .errorOut(errorMapping)
    .requiringPermssions { case (_, channelID, _, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  val restore: AuthedEndpoint[
    (Authentication, ID[Channel], ID[Post], ID[Comment]),
    CommentError,
    RestoreCommentResponse,
    Any
  ] = endpoint
    .name("Restores Comment")
    .summary("Restores specific Comment")
    .description("Schedule specific Comment's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.comments))
    .post
    .in(authHeader)
    .in(prefix / path[ID[Comment]].name("commentID") / "restore")
    .out(jsonBody[RestoreCommentResponse])
    .errorOut(errorMapping)
    .requiringPermssions { case (_, channelID, _, _) =>
      RequiredPermissions.anyOf(Permission.IsOwner, Permission.ModerateChannel(ChannelID(channelID.uuid)))
    }

  // TODO: upvote+revoke

  // TODO: downvote+revoke
}
