package io.branchtalk.discussions.api

import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.api.SubscriptionModels._
import sttp.model.StatusCode

object SubscriptionAPIs {

  private val prefix = "discussions" / "subscriptions"

  private val errorMapping = oneOf[SubscriptionError](
    oneOfVariant[SubscriptionError.BadCredentials](StatusCode.Unauthorized, jsonBody[SubscriptionError.BadCredentials]),
    oneOfVariant[SubscriptionError.NoPermission](StatusCode.Unauthorized, jsonBody[SubscriptionError.NoPermission]),
    oneOfVariant[SubscriptionError.NotFound](StatusCode.NotFound, jsonBody[SubscriptionError.NotFound]),
    oneOfVariant[SubscriptionError.ValidationFailed](StatusCode.BadRequest,
                                                     jsonBody[SubscriptionError.ValidationFailed]
    )
  )

  val newest: AuthedEndpoint[
    Option[Authentication],
    (Option[PaginationOffset], Option[PaginationLimit]),
    PostError,
    Pagination[APIPost],
    Any
  ] = endpoint
    .name("Fetch newest Subscriptions' Posts")
    .summary("Paginate newest Posts for User's Subscriptions")
    .description("Returns Posts for User's subscriptions if logged in or default subscriptions otherwise")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.posts, DiscussionsTags.subscriptions))
    .get
    .securityIn(optAuthHeader)
    .in(prefix / "newest")
    .in(query[Option[PaginationOffset]]("offset"))
    .in(query[Option[PaginationLimit]]("limit"))
    .out(jsonBody[Pagination[APIPost]])
    .errorOut(PostAPIs.errorMapping) // an exception in our API
    .notRequiringPermissions

  val list: AuthedEndpoint[Authentication, Unit, SubscriptionError, APISubscriptions, Any] = endpoint
    .name("List Subscriptions")
    .summary("List Subscriptions for User")
    .description("Returns list of all ChannelID that current User subscribed to")
    .tags(List(DiscussionsTags.domain, DiscussionsTags.subscriptions))
    .get
    .securityIn(authHeader)
    .in(prefix)
    .out(jsonBody[APISubscriptions])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val subscribe: AuthedEndpoint[Authentication, SubscribeRequest, SubscriptionError, SubscribeResponse, Any] =
    endpoint
      .name("Subscribe")
      .summary("Subscribe to Channels")
      .description("Schedule subscribing to Channels")
      .tags(List(DiscussionsTags.domain, DiscussionsTags.subscriptions))
      .put
      .securityIn(authHeader)
      .in(prefix)
      .in(jsonBody[SubscribeRequest])
      .out(jsonBody[SubscribeResponse])
      .errorOut(errorMapping)
      .notRequiringPermissions

  val unsubscribe: AuthedEndpoint[Authentication, UnsubscribeRequest, SubscriptionError, UnsubscribeResponse, Any] =
    endpoint
      .name("Unsubscribe")
      .summary("Unsubscribe from Channels")
      .description("Schedule unsubscribing from Channels")
      .tags(List(DiscussionsTags.domain, DiscussionsTags.subscriptions))
      .delete
      .securityIn(authHeader)
      .in(prefix)
      .in(jsonBody[UnsubscribeRequest])
      .out(jsonBody[UnsubscribeResponse])
      .errorOut(errorMapping)
      .notRequiringPermissions
}
