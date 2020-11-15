package io.branchtalk.discussions.api

import cats.data.{ NonEmptyList, NonEmptySet }
import cats.effect.{ Concurrent, ContextShift, Sync, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.{ APIConfig, PaginationConfig }
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.Post
import io.branchtalk.discussions.reads.{ PostReads, SubscriptionReads }
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CommonError, Paginated }
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.SortedSet

final class SubscriptionServer[F[_]: Http4sServerOptions: Sync: ContextShift: Concurrent: Timer](
  authServices:      AuthServices[F],
  postReads:         PostReads[F],
  subscriptionReads: SubscriptionReads[F],
  apiConfig:         APIConfig,
  paginationConfig:  PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val withErrorHandling = ServerErrorHandling.handleCommonErrors[F, PostError] {
    case CommonError.InvalidCredentials(_) =>
      PostError.BadCredentials("Invalid credentials")
    case CommonError.InsufficientPermissions(msg, _) =>
      PostError.NoPermission(msg)
    case CommonError.NotFound(what, id, _) =>
      PostError.NotFound(s"$what with id=${id.show} could not be found")
    case CommonError.ParentNotExist(what, id, _) =>
      PostError.NotFound(s"Parent $what with id=${id.show} could not be found")
    case CommonError.ValidationFailed(errors, _) =>
      PostError.ValidationFailed(errors)
  }(logger)

  private val newest = SubscriptionAPIs.newest.serverLogic[F].apply { case ((optUser, _), optOffset, optLimit) =>
    withErrorHandling {
      val offset = paginationConfig.resolveOffset(optOffset)
      val limit  = paginationConfig.resolveLimit(optLimit)
      for {
        subscriptionOpt <- optUser.map(_.id).map(userIDUsers2Discussions.get).traverse(subscriptionReads.requireForUser)
        channelIDS = SortedSet.from(subscriptionOpt.map(_.subscriptions).getOrElse(apiConfig.signedOutSubscriptions))
        paginated <- NonEmptySet.fromSet(channelIDS) match {
          case Some(channelIDs) => postReads.paginate(channelIDs, offset.nonNegativeLong, limit.positiveInt)
          case None             => Paginated.empty[Post].pure[F]
        }
      } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
    }
  }

  def endpoints: NonEmptyList[ServerEndpoint[_, PostError, _, Any, F]] = NonEmptyList.of(
    newest
  )

  val routes: HttpRoutes[F] = endpoints.map(_.toRoutes).reduceK
}
