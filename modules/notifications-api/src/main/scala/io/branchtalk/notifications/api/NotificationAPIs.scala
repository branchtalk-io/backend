package io.branchtalk.notifications.api

import io.branchtalk.api.*
import io.branchtalk.api.AuthenticationSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.notifications.api.NotificationModels.*
import io.branchtalk.notifications.model.Notification
import io.branchtalk.shared.model.ID
import sttp.model.StatusCode

object NotificationAPIs {

  private val prefix = "notifications"

  private val errorMapping = oneOf[NotificationError](
    oneOfVariant[NotificationError.BadCredentials](StatusCode.Unauthorized, jsonBody[NotificationError.BadCredentials]),
    oneOfVariant[NotificationError.NoPermission](StatusCode.Forbidden, jsonBody[NotificationError.NoPermission]),
    oneOfVariant[NotificationError.NotFound](StatusCode.NotFound, jsonBody[NotificationError.NotFound]),
    oneOfVariant[NotificationError.ValidationFailed](StatusCode.BadRequest,
                                                     jsonBody[NotificationError.ValidationFailed]
    )
  )

  val list: AuthedEndpoint[
    Authentication,
    (Option[Pagination.Offset], Option[Pagination.Limit], Option[Boolean]),
    NotificationError,
    Pagination[APINotification],
    Any
  ] = endpoint
    .name("List Notifications")
    .summary("Lists notifications for the authenticated user")
    .description("Returns paginated notifications, optionally filtered to unread only")
    .tags(List(NotificationsTags.domain, NotificationsTags.notifications))
    .get
    .securityIn(authHeader)
    .in(prefix)
    .in(query[Option[Pagination.Offset]]("offset"))
    .in(query[Option[Pagination.Limit]]("limit"))
    .in(query[Option[Boolean]]("unread-only"))
    .out(jsonBody[Pagination[APINotification]])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val markRead: AuthedEndpoint[
    Authentication,
    ID[Notification],
    NotificationError,
    MarkReadResponse,
    Any
  ] = endpoint
    .name("Mark Notification Read")
    .summary("Marks a specific notification as read")
    .description("Sets the read timestamp on a specific notification")
    .tags(List(NotificationsTags.domain, NotificationsTags.notifications))
    .put
    .securityIn(authHeader)
    .in(prefix / path[ID[Notification]].name("notificationID") / "read")
    .out(jsonBody[MarkReadResponse])
    .errorOut(errorMapping)
    .notRequiringPermissions

  val markAllRead: AuthedEndpoint[
    Authentication,
    Unit,
    NotificationError,
    MarkAllReadResponse,
    Any
  ] = endpoint
    .name("Mark All Notifications Read")
    .summary("Marks all notifications as read for the authenticated user")
    .description("Sets the read timestamp on all unread notifications for the current user")
    .tags(List(NotificationsTags.domain, NotificationsTags.notifications))
    .put
    .securityIn(authHeader)
    .in(prefix / "read-all")
    .out(jsonBody[MarkAllReadResponse])
    .errorOut(errorMapping)
    .notRequiringPermissions
}
