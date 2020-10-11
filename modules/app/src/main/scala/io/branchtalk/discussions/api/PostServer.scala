package io.branchtalk.discussions.api

import cats.data.NonEmptySet
import cats.effect.{ ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.Pagination
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.discussions.reads.PostReads
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.shared.models.{ CommonError, ID }
import io.branchtalk.shared.models.UUIDGenerator.FastUUIDGenerator
import io.branchtalk.users.services.AuthServices
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](
  authServices:     AuthServices[F],
  reads:            PostReads[F],
  writes:           PostWrites[F],
  paginationConfig: PaginationConfig
) {

  private val logger = Logger(getClass)

  // translation between domains
  private def mapUserID(id: ID[io.branchtalk.users.model.User]): ID[io.branchtalk.discussions.model.User] =
    ID[io.branchtalk.discussions.model.User](id.uuid)

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
          _ <- optAuth.traverse(authServices.authenticateUser) // TODO: so something with it
          offset = paginationConfig.resolveOffset(optOffset)
          limit  = paginationConfig.resolveLimit(optLimit)
          channelIDs <- FastUUIDGenerator
            .create[F] // TODO: fetch from some service
            .map(ID[Channel])
            .map(NonEmptySet.one[ID[Channel]])
          paginated <- reads.paginate(channelIDs, offset.nonNegativeLong, limit.positiveInt)
        } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
      }
  }

  private val create = PostAPIs.create.toRoutes {
    case (auth, createData) =>
      withErrorHandling {
        for {
          userID <- authServices.authenticateUser(auth).map(_.id) // TODO: so something with it
          data = createData.into[Post.Create].withFieldConst(_.authorID, mapUserID(userID)).transform
          result <- writes.createPost(data)
        } yield CreatePostResponse(result.id)
      }
  }

  private val read = PostAPIs.read.toRoutes {
    case (optAuth, postID) =>
      withErrorHandling {
        for {
          _ <- optAuth.traverse(authServices.authenticateUser) // TODO: so something with it
          result <- reads.requireById(postID)
        } yield APIPost.fromDomain(result)
      }
  }

  private val update = PostAPIs.update.toRoutes {
    case (auth, postID, updateData) =>
      withErrorHandling {
        for {
          userID <- authServices.authenticateUser(auth).map(_.id) // TODO: so something with it
          data = updateData
            .into[Post.Update]
            .withFieldConst(_.id, postID)
            .withFieldConst(_.editorID, mapUserID(userID))
            .withFieldRenamed(_.content, _.newContent)
            .withFieldRenamed(_.title, _.newTitle)
            .transform
          result <- writes.updatePost(data)
        } yield UpdatePostResponse(result.id)
      }
  }

  private val delete = PostAPIs.delete.toRoutes {
    case (auth, postID) =>
      withErrorHandling {
        for {
          userID <- authServices.authenticateUser(auth).map(_.id) // TODO: so something with it
          data = Post.Delete(postID, mapUserID(userID))
          result <- writes.deletePost(data)
        } yield DeletePostResponse(result.id)
      }
  }

  val postRoutes: HttpRoutes[F] = newest <+> create <+> read <+> update <+> delete
}
