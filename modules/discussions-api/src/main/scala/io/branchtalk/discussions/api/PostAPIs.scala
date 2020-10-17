package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.models.ID
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.jsoniter._

object PostAPIs {

  private val prefix = "discussions" / "posts"

  private val errorMapping = oneOf[PostError](
    statusMapping[PostError.BadCredentials](StatusCode.Unauthorized, jsonBody[PostError.BadCredentials]),
    statusMapping[PostError.NoPermission](StatusCode.Unauthorized, jsonBody[PostError.NoPermission]),
    statusMapping[PostError.NotFound](StatusCode.NotFound, jsonBody[PostError.NotFound]),
    statusMapping[PostError.ValidationFailed](StatusCode.BadRequest, jsonBody[PostError.ValidationFailed]),
    statusDefaultMapping[PostError](jsonBody[PostError])
  )

  val newest: Endpoint[
    (Option[Authentication], Option[PaginationOffset], Option[PaginationLimit]),
    PostError,
    Pagination[APIPost],
    Nothing
  ] = endpoint
    .name("Fetch newest Posts")
    .summary("Paginate newest Posts")
    .description("Returns Posts for User's subscriptions if logged in or default subscriptions otherwise")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(errorMapping)

  val create: Endpoint[(Authentication, CreatePostRequest), PostError, CreatePostResponse, Nothing] = endpoint
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

  val read: Endpoint[(Option[Authentication], ID[Post]), PostError, APIPost, Nothing] = endpoint
    .name("Fetch Posts")
    .summary("Fetches specific Post")
    .description("Returns specific Post's data")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / path[ID[Post]])
    .out(jsonBody[APIPost])
    .errorOut(errorMapping)

  val update: Endpoint[(Authentication, ID[Post], UpdatePostRequest), PostError, UpdatePostResponse, Nothing] = endpoint
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

  val delete: Endpoint[(Authentication, ID[Post]), PostError, DeletePostResponse, Nothing] = endpoint
    .name("Delete Posts")
    .summary("Deletes specific Post")
    .description("Schedule specific Post's deletion, requires ownership or moderator status")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .put
    .in(authHeader)
    .in(prefix / path[ID[Post]])
    .out(jsonBody[DeletePostResponse])
    .errorOut(errorMapping)

  val endpoints: List[Endpoint[_, _, _, _]] = List(newest, create, read, update, delete)
}
