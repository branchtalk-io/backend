package io.branchtalk.discussions.api

import cats.data.{ NonEmptyList, NonEmptySet }
import cats.effect.{ Concurrent, ContextShift, Sync, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.{ APIConfig, PaginationConfig }
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.api.SubscriptionModels.SubscriptionError
import io.branchtalk.discussions.model.Post
import io.branchtalk.discussions.reads.{ PostReads, SubscriptionReads }
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CommonError, Paginated }
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.SortedSet

final class SubscriptionServer[F[_]: Sync: ContextShift: Concurrent: Timer](
  authServices:      AuthServices[F],
  postReads:         PostReads[F],
  subscriptionReads: SubscriptionReads[F],
  apiConfig:         APIConfig,
  paginationConfig:  PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  implicit private val serverOptions: Http4sServerOptions[F] = SubscriptionServer.serverOptions[F].apply(logger)

  private val withPortErrorHandling = PostServer.serverErrorHandling[F].apply(logger)

  // TODO: split list (by e.g. name) and newest (by creation date)
  private val newest = SubscriptionAPIs.newest.serverLogic[F].apply { case ((optUser, _), optOffset, optLimit) =>
    withPortErrorHandling {
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
object SubscriptionServer {

  def serverOptions[F[_]: Sync: ContextShift]: Logger => Http4sServerOptions[F] =
    ServerOptions.create[F, SubscriptionError](
      _,
      ServerOptions.ErrorHandler[SubscriptionError](
        () => SubscriptionError.ValidationFailed(NonEmptyList.one("Data missing")),
        () => SubscriptionError.ValidationFailed(NonEmptyList.one("Multiple errors")),
        (msg, _) => SubscriptionError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
        (expected, actual) =>
          SubscriptionError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
        errors =>
          SubscriptionError.ValidationFailed(
            NonEmptyList
              .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
              .getOrElse(NonEmptyList.one("Validation failed"))
          )
      )
    )

  def serverErrorHandling[F[_]: Sync]: Logger => ServerErrorHandling[F, SubscriptionError] =
    ServerErrorHandling.handleCommonErrors[F, SubscriptionError] {
      case CommonError.InvalidCredentials(_) =>
        SubscriptionError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        SubscriptionError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        SubscriptionError.NotFound(s"$what with id=${id.show} could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        SubscriptionError.NotFound(s"Parent $what with id=${id.show} could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        SubscriptionError.ValidationFailed(errors)
    }
}
