package io.branchtalk.users.api

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.{ Permission => _, _ }
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.shared.model.{ CommonError, OptionUpdatable, Updatable }
import io.branchtalk.users.api.UserModels._
import io.branchtalk.users.model.{ Permission, User }
import io.branchtalk.users.reads.UserReads
import io.branchtalk.users.writes.UserWrites
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

final class ChannelModerationServer[F[_]: Async](
  authServices:     AuthServices[F],
  userReads:        UserReads[F],
  userWrites:       UserWrites[F],
  paginationConfig: PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val serverOptions: Http4sServerOptions[F] = ChannelModerationServer.serverOptions[F].apply(logger)

  implicit private val errorHandler: ServerErrorHandler[F, UserError] =
    ChannelModerationServer.errorHandler[F].apply(logger)

  private val paginate =
    ChannelModerationAPIs.paginate.serverLogic[F, User] { case (channelID, optOffset, optLimit) =>
      val sortBy  = User.Sorting.NameAlphabetically
      val offset  = paginationConfig.resolveOffset(optOffset)
      val limit   = paginationConfig.resolveLimit(optLimit)
      val filters = List(User.Filter.HasPermission(Permission.ModerateChannel(channelID)))
      for {
        paginated <- userReads.paginate(sortBy, offset.nonNegativeLong, limit.positiveInt, filters)
      } yield Pagination.fromPaginated(paginated.map(APIUser.fromDomain), offset, limit)
    }

  private val grantChannelModeration = ChannelModerationAPIs.grantChannelModeration.serverLogic[F, User].withUser {
    case (moderator, (channelID, GrantModerationRequest(userID))) =>
      val update = User.Update(
        id = userID,
        moderatorID = moderator.id.some,
        newUsername = Updatable.Keep,
        newDescription = OptionUpdatable.Keep,
        newPassword = Updatable.Keep,
        updatePermissions = List(Permission.Update.Add(Permission.ModerateChannel(channelID)))
      )
      for {
        _ <- userWrites.updateUser(update)
      } yield GrantModerationResponse(userID)
  }

  private val revokeChannelModeration = ChannelModerationAPIs.revokeChannelModeration.serverLogic[F, User].withUser {
    case (moderator, (channelID, RevokeModerationRequest(userID))) =>
      val update = User.Update(
        id = userID,
        moderatorID = moderator.id.some,
        newUsername = Updatable.Keep,
        newDescription = OptionUpdatable.Keep,
        newPassword = Updatable.Keep,
        updatePermissions = List(Permission.Update.Remove(Permission.ModerateChannel(channelID)))
      )
      for {
        _ <- userWrites.updateUser(update)
      } yield RevokeModerationResponse(userID)
  }

  def endpoints: NonEmptyList[ServerEndpoint[Any, F]] = NonEmptyList.of[ServerEndpoint[Any, F]](
    paginate,
    grantChannelModeration,
    revokeChannelModeration
  )

  val routes: HttpRoutes[F] = Http4sServerInterpreter(serverOptions).toRoutes(endpoints.toList)
}

object ChannelModerationServer {

  def serverOptions[F[_]: Sync]: Logger => Http4sServerOptions[F] = ServerOptions.create[F, UserError](
    _,
    ServerOptions.ErrorHandler[UserError](
      () => UserError.ValidationFailed(NonEmptyList.one("Data missing")),
      () => UserError.ValidationFailed(NonEmptyList.one("Multiple errors")),
      (msg, _) => UserError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
      (expected, actual) => UserError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
      errors =>
        UserError.ValidationFailed(
          NonEmptyList
            .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
            .getOrElse(NonEmptyList.one("Validation failed"))
        )
    )
  )

  def errorHandler[F[_]: Sync]: Logger => ServerErrorHandler[F, UserError] =
    ServerErrorHandler.handleCommonErrors[F, UserError] {
      case CommonError.InvalidCredentials(_) =>
        UserError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        UserError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        UserError.NotFound(show"$what with id=$id could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        UserError.NotFound(show"Parent $what with id=$id could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        UserError.ValidationFailed(errors)
    }
}
