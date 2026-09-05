package io.branchtalk.discussions.api

import io.branchtalk.api.*
import io.branchtalk.api.AuthenticationSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.discussions.api.CommentModels.APIComment
import io.branchtalk.discussions.api.PostModels.APIPost
import io.branchtalk.discussions.api.SearchModels.*
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.shared.model.ID
import sttp.model.StatusCode

object SearchAPIs {

  private val prefix = "discussions" / "search"

  private[api] val errorMapping = oneOf[SearchError](
    oneOfVariant[SearchError.BadCredentials](StatusCode.Unauthorized, jsonBody[SearchError.BadCredentials]),
    oneOfVariant[SearchError.ValidationFailed](StatusCode.BadRequest, jsonBody[SearchError.ValidationFailed])
  )

  val search: AuthedEndpoint[
    Option[Authentication],
    (String, Option[ID[Channel]], Option[Pagination.Offset], Option[Pagination.Limit]),
    SearchError,
    Pagination[APIPost],
    Any
  ] = endpoint
    .name("Search Posts")
    .summary("Full-text search across Posts")
    .description("Returns paginated Posts matching the search query, optionally filtered by Channel")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.search))
    .get
    .securityIn(optAuthHeader)
    .in(prefix)
    .in(query[String]("q").description("Search query text"))
    .in(query[Option[ID[Channel]]]("channelID").description("Optional Channel ID filter"))
    .in(query[Option[Pagination.Offset]]("offset"))
    .in(query[Option[Pagination.Limit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val searchComments: AuthedEndpoint[
    Option[Authentication],
    (String, Option[ID[Post]], Option[Pagination.Offset], Option[Pagination.Limit]),
    SearchError,
    Pagination[APIComment],
    Any
  ] = endpoint
    .name("Search Comments")
    .summary("Full-text search across Comments")
    .description("Returns paginated Comments matching the search query, optionally filtered by Post")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.search))
    .get
    .securityIn(optAuthHeader)
    .in(prefix / "comments")
    .in(query[String]("q").description("Search query text"))
    .in(query[Option[ID[Post]]]("postID").description("Optional Post ID filter"))
    .in(query[Option[Pagination.Offset]]("offset"))
    .in(query[Option[Pagination.Limit]]("limit"))
    .out(jsonBody[Pagination[APIComment]])
    .errorOut(errorMapping)
    .notRequiringPermissions
}
