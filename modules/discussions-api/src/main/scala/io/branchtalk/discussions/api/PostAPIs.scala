package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.Post
import io.branchtalk.shared.models.ID
import sttp.tapir._
import sttp.tapir.json.jsoniter._

object PostAPIs {

  private val prefix = "discussions" / "posts"

  val newest: Endpoint[
    (Option[Authentication], Option[PaginationOffset], Option[PaginationLimit]),
    PostError,
    Pagination[APIPost],
    Nothing
  ] = endpoint.get
    .in(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(jsonBody[PostError])

  // TODO: hot pagination
  // TODO: controversial pagination

  val create: Endpoint[(Authentication, CreatePostRequest), PostError, CreatePostResponse, Nothing] =
    endpoint.post
      .in(authHeader)
      .in(prefix)
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[CreatePostResponse])
      .errorOut(jsonBody[PostError])

  val read: Endpoint[(Option[Authentication], ID[Post]), PostError, APIPost, Nothing] =
    endpoint.get.in(optAuthHeader).in(prefix / path[ID[Post]]).out(jsonBody[APIPost]).errorOut(jsonBody[PostError])

  val update: Endpoint[(Authentication, ID[Post], UpdatePostRequest), PostError, UpdatePostResponse, Nothing] =
    endpoint.put
      .in(authHeader)
      .in(prefix / path[ID[Post]])
      .in(jsonBody[UpdatePostRequest])
      .out(jsonBody[UpdatePostResponse])
      .errorOut(jsonBody[PostError])

  val delete: Endpoint[(Authentication, ID[Post]), PostError, DeletePostResponse, Nothing] =
    endpoint.put
      .in(authHeader)
      .in(prefix / path[ID[Post]])
      .out(jsonBody[DeletePostResponse])
      .errorOut(jsonBody[PostError])

  val endpoints: List[Endpoint[_, _, _, _]] = List(newest, create, read, update, delete)
}
