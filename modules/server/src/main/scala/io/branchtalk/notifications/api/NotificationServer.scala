package io.branchtalk.notifications.api

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.*
import io.branchtalk.auth.{ *, given }
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.notifications.api.NotificationModels.*
import io.branchtalk.notifications.model.{ Notification, User as NotifUser }
import io.branchtalk.notifications.reads.NotificationReads
import io.branchtalk.notifications.writes.NotificationWrites
import io.branchtalk.mappings.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.model.{ CommonError, ID, Paginated }
import io.branchtalk.users.model.User
import org.http4s.*
import sttp.tapir.server.http4s.*
import sttp.tapir.server.ServerEndpoint

final class NotificationServer[F[_]: Async](
  authServices:      AuthServices[F],
  notificationReads: NotificationReads[F],
  notificationWrites: NotificationWrites[F],
  paginationConfig:  PaginationConfig
) {

  private given AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val serverOptions: Http4sServerOptions[F] = NotificationServer.serverOptions[F](logger)

  private given errorHandler: ServerErrorHandler[F, NotificationError] = NotificationServer.errorHandler[F](logger)

  private val list = NotificationAPIs.list.serverLogic[F, User]
    .withUser { case (user, (optOffset, optLimit, optUnreadOnly)) =>
      val recipientID = ID[NotifUser](user.id.unwrap)
      val sorting     = Notification.Sorting.Newest
      val offset      = paginationConfig.resolveOffset(optOffset)
      val limit       = paginationConfig.resolveLimit(optLimit)
      val unreadOnly  = optUnreadOnly.getOrElse(false)
      for {
        paginated <- notificationReads.paginate(recipientID, unreadOnly, sorting, offset, limit)
      } yield Pagination.fromPaginated(paginated.map(APINotification.fromDomain), offset, limit)
    }

  private val markRead = NotificationAPIs.markRead.serverLogic[F, User]
    .withUser { case (user, notificationID) =>
      val command = Notification.MarkRead(
        id = notificationID,
        userID = ID[NotifUser](user.id.unwrap)
      )
      for {
        _ <- notificationWrites.markRead(command)
      } yield MarkReadResponse(notificationID)
    }

  private val markAllRead = NotificationAPIs.markAllRead.serverLogic[F, User]
    .justUser { user =>
      val command = Notification.MarkAllRead(
        recipientID = ID[NotifUser](user.id.unwrap)
      )
      for {
        _ <- notificationWrites.markAllRead(command)
      } yield MarkAllReadResponse(0L) // count is not tracked in async flow
    }

  def endpoints: NonEmptyList[ServerEndpoint[Any, F]] = NonEmptyList.of[ServerEndpoint[Any, F]](
    list,
    markRead,
    markAllRead
  )

  val routes: HttpRoutes[F] = Http4sServerInterpreter(serverOptions).toRoutes(endpoints.toList)
}
object NotificationServer {

  def serverOptions[F[_]](using Sync[F]): Logger => Http4sServerOptions[F] =
    ServerOptions.create[F, NotificationError](
      _,
      ServerOptions.ErrorHandler[NotificationError](
        () => NotificationError.ValidationFailed(NonEmptyList.one("Data missing")),
        () => NotificationError.ValidationFailed(NonEmptyList.one("Multiple errors")),
        (msg, _) => NotificationError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
        (expected, actual) =>
          NotificationError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
        errors =>
          NotificationError.ValidationFailed(
            NonEmptyList
              .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
              .getOrElse(NonEmptyList.one("Validation failed"))
          )
      )
    )

  def errorHandler[F[_]](using Sync[F]): Logger => ServerErrorHandler[F, NotificationError] =
    ServerErrorHandler.handleCommonErrors[F, NotificationError] {
      case CommonError.InvalidCredentials(_) =>
        NotificationError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        NotificationError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        NotificationError.NotFound(show"$what with id=$id could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        NotificationError.NotFound(show"Parent $what with id=$id could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        NotificationError.ValidationFailed(errors)
    }
}
