package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import cats.effect.{ Async, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.*
import io.branchtalk.auth.{ *, given }
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.PostModels.APIPost
import io.branchtalk.discussions.api.SearchModels.*
import io.branchtalk.discussions.reads.PostReads
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.shared.model.{ CommonError, Paginated }
import io.branchtalk.users.model.User
import org.http4s.*
import sttp.tapir.server.http4s.*
import sttp.tapir.server.ServerEndpoint

final class SearchServer[F[_]: Async](
  authServices:     AuthServices[F],
  postReads:        PostReads[F],
  paginationConfig: PaginationConfig
) {

  private given AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val serverOptions: Http4sServerOptions[F] = SearchServer.serverOptions[F](logger)

  private given errorHandler: ServerErrorHandler[F, SearchError] = SearchServer.errorHandler[F](logger)

  private val search = SearchAPIs.search.serverLogic[F, Option[User]] { case (q, channelID, optOffset, optLimit) =>
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      paginated <- postReads.search(q, channelID, offset, limit)
    } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
  }

  def endpoints: NonEmptyList[ServerEndpoint[Any, F]] = NonEmptyList.of[ServerEndpoint[Any, F]](
    search
  )

  val routes: HttpRoutes[F] = Http4sServerInterpreter(serverOptions).toRoutes(endpoints.toList)
}
object SearchServer {

  def serverOptions[F[_]](using Sync[F]): Logger => Http4sServerOptions[F] = ServerOptions.create[F, SearchError](
    _,
    ServerOptions.ErrorHandler[SearchError](
      () => SearchError.ValidationFailed(NonEmptyList.one("Data missing")),
      () => SearchError.ValidationFailed(NonEmptyList.one("Multiple errors")),
      (msg, _) => SearchError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
      (expected, actual) => SearchError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
      errors =>
        SearchError.ValidationFailed(
          NonEmptyList
            .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
            .getOrElse(NonEmptyList.one("Validation failed"))
        )
    )
  )

  def errorHandler[F[_]](using Sync[F]): Logger => ServerErrorHandler[F, SearchError] =
    ServerErrorHandler.handleCommonErrors[F, SearchError] {
      case CommonError.InvalidCredentials(_) =>
        SearchError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        SearchError.BadCredentials(msg)
      case CommonError.NotFound(what, id, _) =>
        SearchError.ValidationFailed(NonEmptyList.one(show"$what with id=$id could not be found"))
      case CommonError.ParentNotExist(what, id, _) =>
        SearchError.ValidationFailed(NonEmptyList.one(show"Parent $what with id=$id could not be found"))
      case CommonError.ValidationFailed(errors, _) =>
        SearchError.ValidationFailed(errors)
    }
}
