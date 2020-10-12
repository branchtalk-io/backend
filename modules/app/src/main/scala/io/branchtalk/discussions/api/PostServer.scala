package io.branchtalk.discussions.api

import cats.data.NonEmptySet
import cats.effect.{ ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.Pagination
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
  usersReads:        PostReads[F],
  usersWrites:       PostWrites[F],
  subscriptionReads: SubscriptionReads[F],
  paginationConfig:  PaginationConfig
) {

  private val logger = Logger(getClass)

  // TODO: make it configurable and make new users subscribe to these configured values
  private def defaultSubscriptions: Set[ID[Channel]] = Set.empty

  private def withErrorHandling[A](fa: F[A]): F[Either[PostError, A]] = fa.map(_.asRight[PostError]).handleErrorWith {
    case CommonError.InvalidCredentials(_) =>
      (PostError.BadCredentials("Invalid credentials"): PostError).asLeft[A].pure[F]
    case CommonError.InsufficientPermissions(msg, _) =>
      (PostError.NoPermission(msg): PostError).asLeft[A].pure[F]
    case CommonError.NotFound(what, id, _) =>
      (PostError.NotFound(s"$what with id=${id.show} could not be found"): PostError).asLeft[A].pure[F]
    case CommonError.ParentNotExist(what, id, _) =>
      (PostError.NotFound(s"Parent $what with id=${id.show} could not be found"): PostError).asLeft[A].pure[F]
    case CommonError.ValidationFailed(errors, _) =>
      (PostError.ValidationFailed(errors): PostError).asLeft[A].pure[F]
    case error: Throwable =>
      logger.warn("Unhandled error in domain code", error)
      error.raiseError[F, Either[PostError, A]]
  }

  private val newest = PostAPIs.newest.toRoutes {
    case (optAuth, optOffset, optLimit) =>
      withErrorHandling {
        for {
          optUser <- optAuth.traverse(authServices.authenticateUser) // TODO: so something with it
          offset = paginationConfig.resolveOffset(optOffset)
          limit  = paginationConfig.resolveLimit(optLimit)
          subscriptionOpt <- optUser
            .map(_.id)
            .map(userIDUsers2Discussions.get)
            .traverse(subscriptionReads.requireForUser)
          channelIDS = SortedSet.from(subscriptionOpt.map(_.subscriptions).getOrElse(defaultSubscriptions))
          paginated <- NonEmptySet.fromSet(channelIDS) match {
            case Some(channelIDs) => usersReads.paginate(channelIDs, offset.nonNegativeLong, limit.positiveInt)
            case None             => Paginated.empty[Post].pure[F]
          }
        } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
      }
  }

  private val create = PostAPIs.create.toRoutes {
    case (auth, createData) =>
      withErrorHandling {
        for {
          userID <- authServices.authenticateUser(auth).map(_.id) // TODO: so something with it
          data = createData.into[Post.Create].withFieldConst(_.authorID, userIDUsers2Discussions.get(userID)).transform
          result <- usersWrites.createPost(data)
        } yield CreatePostResponse(result.id)
      }
  }

  private val read = PostAPIs.read.toRoutes {
    case (optAuth, postID) =>
      withErrorHandling {
        for {
          _ <- optAuth.traverse(authServices.authenticateUser) // TODO: so something with it
          result <- usersReads.requireById(postID)
        } yield APIPost.fromDomain(result)
      }
  }

  // TODO: authorize - author or moderator
  private val update = PostAPIs.update.toRoutes {
    case (auth, postID, updateData) =>
      withErrorHandling {
        for {
          userID <- authServices.authenticateUser(auth).map(_.id) // TODO: so something with it
          data = updateData
            .into[Post.Update]
            .withFieldConst(_.id, postID)
            .withFieldConst(_.editorID, userIDUsers2Discussions.get(userID))
            .withFieldRenamed(_.content, _.newContent)
            .withFieldRenamed(_.title, _.newTitle)
            .transform
          result <- usersWrites.updatePost(data)
        } yield UpdatePostResponse(result.id)
      }
  }

  // TODO: authorize - author or moderator
  private val delete = PostAPIs.delete.toRoutes {
    case (auth, postID) =>
      withErrorHandling {
        for {
          userID <- authServices.authenticateUser(auth).map(_.id) // TODO: so something with it
          data = Post.Delete(postID, userIDUsers2Discussions.get(userID))
          result <- usersWrites.deletePost(data)
        } yield DeletePostResponse(result.id)
      }
  }

  val postRoutes: HttpRoutes[F] = newest <+> create <+> read <+> update <+> delete
}
