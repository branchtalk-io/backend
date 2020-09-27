package io.branchtalk.discussions.api

import cats.data.NonEmptySet
import cats.effect.{ ContextShift, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api.{ Authentication, Pagination }
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.PostModels.{ APIPost, PostError }
import io.branchtalk.discussions.model.{ Channel, Post, User }
import io.branchtalk.discussions.reads.PostReads
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.shared.models.{ CommonError, ID }
import io.branchtalk.shared.models.UUIDGenerator.FastUUIDGenerator
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

import scala.annotation.nowarn

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](
  reads:            PostReads[F],
  writes:           PostWrites[F],
  paginationConfig: PaginationConfig
) {

  private val logger = Logger(getClass)

  // TODO: create some AuthService which would take Authentication and list of accesses to validate
  @nowarn("cat=unused")
  private def mockAuth(auth: Authentication): F[ID[User]] = FastUUIDGenerator.create[F].map(ID[User])

  private def withErrorHandling[A](fa: F[A]): F[Either[PostError, A]] = fa.map(_.asRight[PostError]).handleErrorWith {
    case CommonError.NotFound(what, id, _) =>
      (PostError.NotFound(s"$what with id=$id could not be found"): PostError).asLeft[A].pure[F]
    case CommonError.ParentNotExist(what, id, _) =>
      (PostError.NotFound(s"Parent $what with id=$id could not be found"): PostError).asLeft[A].pure[F]
    case CommonError.ValidationFailed(errors, _) =>
      (PostError.ValidationFailed(errors): PostError).asLeft[A].pure[F]
    case error: Throwable =>
      logger.warn("Unhandled error in domain code", error)
      error.raiseError[F, Either[PostError, A]]
  }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null")) // temporarily
  private val newest = PostAPIs.newest.toRoutes {
    case (optAuth, optOffset, optLimit) =>
      withErrorHandling {
        for {
          _ <- optAuth.traverse(mockAuth)
          offset = paginationConfig.resolveOffset(optOffset)
          limit  = paginationConfig.resolveLimit(optLimit)
          channelIDs <- FastUUIDGenerator
            .create[F] // TODO: fetch from some service
            .map(ID[Channel])
            .map(NonEmptySet.one[ID[Channel]])
          paginated <- reads.paginate(channelIDs, offset.value, limit.value)
        } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
      }
  }

  private val create = PostAPIs.create.toRoutes {
    case (auth, createData) =>
      withErrorHandling {
        for {
          userID <- mockAuth(auth)
          data = createData.into[Post.Create].withFieldConst(_.authorID, userID).transform
          result <- writes.createPost(data)
        } yield PostModels.CreatePostResponse(result.id)
      }
  }

  private val read = PostAPIs.read.toRoutes {
    case (optAuth, postID) =>
      withErrorHandling {
        for {
          _ <- optAuth.traverse(mockAuth)
          result <- reads.requireById(postID)
        } yield APIPost.fromDomain(result)
      }
  }

  private val update = PostAPIs.update.toRoutes {
    case (auth, postID, updateData) =>
      withErrorHandling {
        for {
          userID <- mockAuth(auth)
          data = updateData
            .into[Post.Update]
            .withFieldConst(_.id, postID)
            .withFieldConst(_.editorID, userID)
            .withFieldRenamed(_.content, _.newContent)
            .withFieldRenamed(_.title, _.newTitle)
            .transform
          result <- writes.updatePost(data)
        } yield PostModels.UpdatePostResponse(result.id)
      }
  }

  private val delete = PostAPIs.delete.toRoutes {
    case (auth, postID) =>
      withErrorHandling {
        for {
          userID <- mockAuth(auth)
          data = Post.Delete(postID, userID)
          result <- writes.deletePost(data)
        } yield PostModels.DeletePostResponse(result.id)
      }
  }

  val postRoutes: HttpRoutes[F] = newest <+> create <+> read <+> update <+> delete
}
