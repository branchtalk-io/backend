package io.branchtalk.discussions.api

import cats.data.NonEmptySet
import cats.effect.{ ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.{ Pagination, Permission, ServerErrorHandling }
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.discussions.reads.{ PostReads, SubscriptionReads }
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CommonError, ID, Paginated }
import io.branchtalk.users.services.AuthServices
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

import scala.collection.immutable.SortedSet

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](
  authServices:      AuthServices[F],
  postReads:         PostReads[F],
  postWrites:        PostWrites[F],
  subscriptionReads: SubscriptionReads[F],
  paginationConfig:  PaginationConfig
) {

  private val logger = Logger(getClass)

  private def defaultSubscriptions: Set[ID[Channel]] = Set.empty

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

  private val newest = PostAPIs.newest.toRoutes {
    case (optAuth, optOffset, optLimit) =>
      withErrorHandling {
        for {
          optUser <- optAuth.traverse(authServices.authenticateUser)
          offset = paginationConfig.resolveOffset(optOffset)
          limit  = paginationConfig.resolveLimit(optLimit)
          subscriptionOpt <- optUser
            .map(_.id)
            .map(userIDUsers2Discussions.get)
            .traverse(subscriptionReads.requireForUser)
          channelIDS = SortedSet.from(subscriptionOpt.map(_.subscriptions).getOrElse(defaultSubscriptions))
          paginated <- NonEmptySet.fromSet(channelIDS) match {
            case Some(channelIDs) => postReads.paginate(channelIDs, offset.nonNegativeLong, limit.positiveInt)
            case None             => Paginated.empty[Post].pure[F]
          }
        } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
      }
  }

  private val create = PostAPIs.create.toRoutes {
    case (auth, createData) =>
      withErrorHandling {
        for {
          userID <- authServices.authenticateUser(auth).map(_.id)
          data = createData.into[Post.Create].withFieldConst(_.authorID, userIDUsers2Discussions.get(userID)).transform
          result <- postWrites.createPost(data)
        } yield CreatePostResponse(result.id)
      }
  }

  private val read = PostAPIs.read.toRoutes {
    case (optAuth, postID) =>
      withErrorHandling {
        for {
          _ <- optAuth.traverse(authServices.authenticateUser)
          result <- postReads.requireById(postID)
        } yield APIPost.fromDomain(result)
      }
  }

  private val update = PostAPIs.update.toRoutes {
    case (auth, postID, updateData) =>
      withErrorHandling {
        for {
          post <- postReads.requireById(postID)
          userID <- authServices
            .authorizeUser(auth, Permission.ModerateChannel(channelIDApi2Discussions.reverseGet(post.data.channelID)))
            .map(_.id)
          data = updateData
            .into[Post.Update]
            .withFieldConst(_.id, postID)
            .withFieldConst(_.editorID, userIDUsers2Discussions.get(userID))
            .withFieldRenamed(_.content, _.newContent)
            .withFieldRenamed(_.title, _.newTitle)
            .transform
          result <- postWrites.updatePost(data)
        } yield UpdatePostResponse(result.id)
      }
  }

  private val delete = PostAPIs.delete.toRoutes {
    case (auth, postID) =>
      withErrorHandling {
        for {
          post <- postReads.requireById(postID)
          userID <- authServices
            .authorizeUser(auth, Permission.ModerateChannel(channelIDApi2Discussions.reverseGet(post.data.channelID)))
            .map(_.id)
          data = Post.Delete(postID, userIDUsers2Discussions.get(userID))
          result <- postWrites.deletePost(data)
        } yield DeletePostResponse(result.id)
      }
  }

  val postRoutes: HttpRoutes[F] = newest <+> create <+> read <+> update <+> delete
}
