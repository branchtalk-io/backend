package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.ChannelModels._
import io.branchtalk.discussions.model.Channel
import io.branchtalk.discussions.reads.ChannelReads
import io.branchtalk.discussions.writes.ChannelWrites
import io.branchtalk.mappings.userIDUsers2Discussions
import io.branchtalk.shared.model.{ CommonError, CreationScheduled }
import io.branchtalk.users.model.User
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

final class ChannelServer[F[_]: Async](
  authServices:     AuthServices[F],
  channelReads:     ChannelReads[F],
  channelWrites:    ChannelWrites[F],
  paginationConfig: PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  implicit val serverOptions: Http4sServerOptions[F] = ChannelServer.serverOptions[F].apply(logger)

  implicit private val errorHandler: ServerErrorHandler[F, ChannelError] = ChannelServer.errorHandler[F].apply(logger)

  private val paginate = ChannelAPIs.paginate.serverLogic[F, Option[User]] { case (optOffset, optLimit) =>
    val sortBy = Channel.Sorting.Newest
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      paginated <- channelReads.paginate(sortBy, offset.nonNegativeLong, limit.positiveInt)
    } yield Pagination.fromPaginated(paginated.map(APIChannel.fromDomain), offset, limit)
  }

  private val create = ChannelAPIs.create.serverLogic[F, User].withUser { (user, createData) =>
    val userID = user.id
    val data =
      createData.into[Channel.Create].withFieldConst(_.authorID, userIDUsers2Discussions.get(userID)).transform
    for {
      CreationScheduled(channelID) <- channelWrites.createChannel(data)
    } yield CreateChannelResponse(channelID)
  }

  private val read = ChannelAPIs.read.serverLogic[F, Option[User]] { channelID =>
    for {
      channel <- channelReads.requireById(channelID)
    } yield APIChannel.fromDomain(channel)
  }

  private val update = ChannelAPIs.update.serverLogic[F, User].withUser { case (user, (channelID, updateData)) =>
    val userID = user.id
    val data = updateData
      .into[Channel.Update]
      .withFieldConst(_.id, channelID)
      .withFieldConst(_.editorID, userIDUsers2Discussions.get(userID))
      .transform
    for {
      _ <- channelWrites.updateChannel(data)
    } yield UpdateChannelResponse(channelID)
  }

  private val delete = ChannelAPIs.delete.serverLogic[F, User].withUser { (user, channelID) =>
    val userID = user.id
    val data   = Channel.Delete(channelID, userIDUsers2Discussions.get(userID))
    for {
      _ <- channelWrites.deleteChannel(data)
    } yield DeleteChannelResponse(channelID)
  }

  private val restore = ChannelAPIs.restore.serverLogic[F, User].withUser { (user, channelID) =>
    val userID = user.id
    val data   = Channel.Restore(channelID, userIDUsers2Discussions.get(userID))
    for {
      _ <- channelWrites.restoreChannel(data)
    } yield RestoreChannelResponse(channelID)
  }

  def endpoints: NonEmptyList[ServerEndpoint[Any, F]] = NonEmptyList.of[ServerEndpoint[Any, F]](
    paginate,
    create,
    read,
    update,
    delete,
    restore
  )

  val routes: HttpRoutes[F] = Http4sServerInterpreter(serverOptions).toRoutes(endpoints.toList)
}
object ChannelServer {

  def serverOptions[F[_]: Sync]: Logger => Http4sServerOptions[F] = ServerOptions.create[F, ChannelError](
    _,
    ServerOptions.ErrorHandler[ChannelError](
      () => ChannelError.ValidationFailed(NonEmptyList.one("Data missing")),
      () => ChannelError.ValidationFailed(NonEmptyList.one("Multiple errors")),
      (msg, _) => ChannelError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
      (expected, actual) => ChannelError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
      errors =>
        ChannelError.ValidationFailed(
          NonEmptyList
            .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
            .getOrElse(NonEmptyList.one("Validation failed"))
        )
    )
  )

  def errorHandler[F[_]: Sync]: Logger => ServerErrorHandler[F, ChannelError] =
    ServerErrorHandler.handleCommonErrors[F, ChannelError] {
      case CommonError.InvalidCredentials(_) =>
        ChannelError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        ChannelError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        ChannelError.NotFound(show"$what with id=$id could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        ChannelError.NotFound(show"Parent $what with id=$id could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        ChannelError.ValidationFailed(errors)
    }
}
