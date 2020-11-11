package io.branchtalk.discussions.api

import cats.data.{ NonEmptyList, NonEmptySet }
import cats.effect.{ ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.{ APIConfig, PaginationConfig }
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.discussions.reads.{ PostReads, SubscriptionReads }
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CodePosition, CommonError, ID, Paginated }
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.SortedSet

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](
  authServices:      AuthServices[F],
  postReads:         PostReads[F],
  postWrites:        PostWrites[F],
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

  private def checkOwnership(post: Post, channelID: ID[Channel])(implicit codePosition: CodePosition) =
    if (post.data.channelID === channelID) Sync[F].unit
    else Sync[F].raiseError(CommonError.ParentNotExist("Channel", channelID, codePosition))

  private val newest = PostAPIs.newest.optAuthenticated.serverLogic { case ((_, _), channelID, optOffset, optLimit) =>
    withErrorHandling {
      val offset     = paginationConfig.resolveOffset(optOffset)
      val limit      = paginationConfig.resolveLimit(optLimit)
      val channelIDS = SortedSet(channelID)
      for {
        // TODO: extract it into a separate endpoint for subscriptions!
        // subscriptionOpt <- optUser.map(_.id).map(userIDUsers2Discussions.get).traverse(subscriptionReads.requireForUser)
        // channelIDS = SortedSet.from(subscriptionOpt.map(_.subscriptions).getOrElse(apiConfig.signedOutSubscriptions))
        paginated <- NonEmptySet.fromSet(channelIDS) match {
          case Some(channelIDs) => postReads.paginate(channelIDs, offset.nonNegativeLong, limit.positiveInt)
          case None             => Paginated.empty[Post].pure[F]
        }
      } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
    }
  }

  private val create = PostAPIs.create.authenticated.serverLogic { case ((user, _), channelID, createData) =>
    withErrorHandling {
      val userID = user.id
      val data = createData
        .into[Post.Create]
        .withFieldConst(_.authorID, userIDUsers2Discussions.get(userID))
        .withFieldConst(_.channelID, channelID)
        .transform
      for {
        creationScheduled <- postWrites.createPost(data)
      } yield CreatePostResponse(creationScheduled.id)
    }
  }

  // TODO: validate that post belongs to channel
  private val read = PostAPIs.read.optAuthenticated.serverLogic { case ((_, _), channelID, postID) =>
    withErrorHandling {
      for {
        post <- postReads.requireById(postID)
        _ <- checkOwnership(post, channelID)
      } yield APIPost.fromDomain(post)
    }
  }

  private val update = PostAPIs.update.authorized
    .withOwnership { case (_, _, postID, _, _) =>
      postReads.requireById(postID).map(_.data.authorID).map(userIDApi2Discussions.reverseGet)
    }
    .serverLogic { case ((user, _), channelID, postID, updateData) =>
      withErrorHandling {
        val userID = user.id
        for {
          post <- postReads.requireById(postID)
          _ <- checkOwnership(post, channelID)
          data = updateData
            .into[Post.Update]
            .withFieldConst(_.id, postID)
            .withFieldConst(_.editorID, userIDUsers2Discussions.get(userID))
            .withFieldRenamed(_.newContent, _.newContent)
            .withFieldRenamed(_.newTitle, _.newTitle)
            .transform
          result <- postWrites.updatePost(data)
        } yield UpdatePostResponse(result.id)
      }
    }

  private val delete = PostAPIs.delete.authorized
    .withOwnership { case (_, _, postID, _) =>
      postReads.requireById(postID).map(_.data.authorID).map(userIDApi2Discussions.reverseGet)
    }
    .serverLogic { case ((user, _), channelID, postID) =>
      withErrorHandling {
        val userID = user.id
        for {
          post <- postReads.requireById(postID)
          _ <- checkOwnership(post, channelID)
          data = Post.Delete(postID, userIDUsers2Discussions.get(userID))
          result <- postWrites.deletePost(data)
        } yield DeletePostResponse(result.id)
      }
    }

  def endpoints: NonEmptyList[ServerEndpoint[_, PostError, _, Nothing, F]] = NonEmptyList.of(
    newest,
    create,
    read,
    update,
    delete
  )

  val routes: HttpRoutes[F] = endpoints.map(_.toRoutes).reduceK
}
