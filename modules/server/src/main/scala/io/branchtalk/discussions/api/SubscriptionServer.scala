package io.branchtalk.discussions.api

import cats.data.{ NonEmptyList, NonEmptySet }
import cats.effect.{ Concurrent, ContextShift, Sync, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.{ APIConfig, PaginationConfig }
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.api.SubscriptionModels._
import io.branchtalk.discussions.model.{ Post, Subscription }
import io.branchtalk.discussions.reads.{ PostReads, SubscriptionReads }
import io.branchtalk.discussions.writes.SubscriptionWrites
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CommonError, Paginated }
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.SortedSet

final class SubscriptionServer[F[_]: Sync: ContextShift: Concurrent: Timer](
  authServices:       AuthServices[F],
  postReads:          PostReads[F],
  subscriptionReads:  SubscriptionReads[F],
  subscriptionWrites: SubscriptionWrites[F],
  apiConfig:          APIConfig,
  paginationConfig:   PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  implicit private val serverOptions: Http4sServerOptions[F] = SubscriptionServer.serverOptions[F].apply(logger)

  implicit private val postErrorHandler: ServerErrorHandler[F, PostError] = PostServer.errorHandler[F].apply(logger)

  implicit private val errorHandler: ServerErrorHandler[F, SubscriptionError] =
    SubscriptionServer.errorHandler[F].apply(logger)

  private val newest = SubscriptionAPIs.newest.serverLogic[F].apply { case ((optUser, _), optOffset, optLimit) =>
    val sortBy = Post.Sorting.Newest
    val offset = paginationConfig.resolveOffset(optOffset)
    val limit  = paginationConfig.resolveLimit(optLimit)
    for {
      subscriptionOpt <- optUser.map(_.id).map(userIDUsers2Discussions.get).traverse(subscriptionReads.requireForUser)
      channelIDS = SortedSet.from(subscriptionOpt.map(_.subscriptions).getOrElse(apiConfig.signedOutSubscriptions))
      paginated <- NonEmptySet.fromSet(channelIDS) match {
        case Some(channelIDs) => postReads.paginate(channelIDs, sortBy, offset.nonNegativeLong, limit.positiveInt)
        case None             => Paginated.empty[Post].pure[F]
      }
    } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
  }

  private val list = SubscriptionAPIs.list.serverLogic[F].apply { case (user, _) =>
    for {
      subscription <- subscriptionReads.requireForUser(userIDUsers2Discussions.get(user.id))
    } yield APISubscriptions(subscription.subscriptions.toList)
  }

  private val subscribe = SubscriptionAPIs.subscribe.serverLogic[F].apply { case ((user, _), subscribeData) =>
    val userID = userIDUsers2Discussions.get(user.id)
    val data   = Subscription.Subscribe(userID, subscribeData.channels.toSet)
    for {
      result <- subscriptionWrites.subscribe(data)
    } yield SubscribeResponse(result.subscription.subscriptions.toList)
  }

  private val unsubscribe = SubscriptionAPIs.unsubscribe.serverLogic[F].apply { case ((user, _), unsubscribeData) =>
    val userID = userIDUsers2Discussions.get(user.id)
    val data   = Subscription.Unsubscribe(userID, unsubscribeData.channels.toSet)
    for {
      result <- subscriptionWrites.unsubscribe(data)
    } yield UnsubscribeResponse(result.subscription.subscriptions.toList)
  }

  def endpoints: NonEmptyList[ServerEndpoint[_, _, _, Any, F]] = NonEmptyList.of(
    newest,
    list,
    subscribe,
    unsubscribe
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

  def errorHandler[F[_]: Sync]: Logger => ServerErrorHandler[F, SubscriptionError] =
    ServerErrorHandler.handleCommonErrors[F, SubscriptionError] {
      case CommonError.InvalidCredentials(_) =>
        SubscriptionError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        SubscriptionError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        SubscriptionError.NotFound(show"$what with id=$id could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        SubscriptionError.NotFound(show"Parent $what with id=$id could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        SubscriptionError.ValidationFailed(errors)
    }
}
