package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.discussions.api.models._
import io.branchtalk.shared.models.ID
import sttp.tapir._
import sttp.tapir.json.jsoniter._

object posts { // scalastyle:ignore object.name

  private val prefix = "discussions" / "posts"

  val newest: Endpoint[
    (Option[Authentication], Option[PaginationOffset], Option[PaginationLimit]),
    PostErrors,
    Pagination[APIPost],
    Nothing
  ] = endpoint.get
    .in(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(jsonBody[PostErrors])

  // TODO: hot pagination
  // TODO: controversial pagination

  val create: Endpoint[(Authentication, ID[APIPost], CreatePostRequest), PostErrors, CreatePostResponse, Nothing] =
    endpoint.post
      .in(authHeader)
      .in(prefix / path[ID[APIPost]])
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[CreatePostResponse])
      .errorOut(jsonBody[PostErrors])

  val read: Endpoint[(Option[Authentication], ID[APIPost]), PostErrors, APIPost, Nothing] =
    endpoint.get.in(optAuthHeader).in(prefix / path[ID[APIPost]]).out(jsonBody[APIPost]).errorOut(jsonBody[PostErrors])

  val update: Endpoint[(Authentication, ID[APIPost], UpdatePostRequest), PostErrors, UpdatePostResponse, Nothing] =
    endpoint.put
      .in(authHeader)
      .in(prefix / path[ID[APIPost]])
      .in(jsonBody[UpdatePostRequest])
      .out(jsonBody[UpdatePostResponse])
      .errorOut(jsonBody[PostErrors])

  val delete: Endpoint[(Authentication, ID[APIPost]), PostErrors, DeletePostResponse, Nothing] =
    endpoint.put
      .in(authHeader)
      .in(prefix / path[ID[APIPost]])
      .out(jsonBody[DeletePostResponse])
      .errorOut(jsonBody[PostErrors])
}
