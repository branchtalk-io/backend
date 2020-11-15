package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.discussions.api.PostModels._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.jsoniter._

object SubscriptionAPIs {

  private val prefix = "discussions" / "subscriptions"

  // TODO: create a separate Error algebra here
  private val errorMapping = oneOf[PostError](
    statusMapping[PostError.BadCredentials](StatusCode.Unauthorized, jsonBody[PostError.BadCredentials]),
    statusMapping[PostError.NoPermission](StatusCode.Unauthorized, jsonBody[PostError.NoPermission]),
    statusMapping[PostError.NotFound](StatusCode.NotFound, jsonBody[PostError.NotFound]),
    statusMapping[PostError.ValidationFailed](StatusCode.BadRequest, jsonBody[PostError.ValidationFailed]),
    statusDefaultMapping[PostError](jsonBody[PostError])
  )

  val newest: AuthedEndpoint[
    (Option[Authentication], Option[PaginationOffset], Option[PaginationLimit]),
    PostError,
    Pagination[APIPost],
    Any
  ] = endpoint
    .name("Fetch newest Subscriptions")
    .summary("Paginate newest Posts for User's Subscriptions")
    .description("Returns Posts for User's subscriptions if logged in or default subscriptions otherwise")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts))
    .get
    .in(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(errorMapping)
    .notRequiringPermissions
}
