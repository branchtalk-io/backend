package io.branchtalk.discussions.api

import cats.data.{ NonEmptyList, NonEmptySet }
import cats.effect.{ Concurrent, ContextShift, Sync, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.Post
import io.branchtalk.discussions.reads.PostReads
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.mappings._
import io.branchtalk.shared.models.{ CommonError, Paginated }
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.SortedSet

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift: Concurrent: Timer](
  authServices:     AuthServices[F],
  postReads:        PostReads[F],
  postWrites:       PostWrites[F],
  paginationConfig: PaginationConfig
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

  private val newest = new AuthOps(PostAPIs.newest).optAuthenticated.serverLogic {
    case ((_, _), channelID, optOffset, optLimit) =>
      withErrorHandling {
        val offset     = paginationConfig.resolveOffset(optOffset)
        val limit      = paginationConfig.resolveLimit(optLimit)
        val channelIDS = SortedSet(channelID)
        for {
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

  private val read = PostAPIs.read.optAuthenticated
    .withOwnership { case (_, channelID, postID) =>
      postReads
        .requireById(postID)
        .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
        .void
    }
    .serverLogic { case ((_, _), _, postID) =>
      withErrorHandling {
        for {
          post <- postReads.requireById(postID)
        } yield APIPost.fromDomain(post)
      }
    }

  private val update = PostAPIs.update.authorized
    .withOwnership { case (_, channelID, postID, _, _) =>
      postReads
        .requireById(postID)
        .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
        .map(_.data.authorID)
        .map(userIDApi2Discussions.reverseGet)
    }
    .serverLogic { case ((user, _), _, postID, updateData) =>
      withErrorHandling {
        val userID = user.id
        val data = updateData
          .into[Post.Update]
          .withFieldConst(_.id, postID)
          .withFieldConst(_.editorID, userIDUsers2Discussions.get(userID))
          .withFieldRenamed(_.newContent, _.newContent)
          .withFieldRenamed(_.newTitle, _.newTitle)
          .transform
        for {
          result <- postWrites.updatePost(data)
        } yield UpdatePostResponse(result.id)
      }
    }

  private val delete = PostAPIs.delete.authorized
    .withOwnership { case (_, channelID, postID, _) =>
      postReads
        .requireById(postID)
        .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
        .map(_.data.authorID)
        .map(userIDApi2Discussions.reverseGet)
    }
    .serverLogic { case ((user, _), _, postID) =>
      withErrorHandling {
        val userID = user.id
        val data   = Post.Delete(postID, userIDUsers2Discussions.get(userID))
        for {
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

  val routes: HttpRoutes[F] = endpoints.map(_.asR[Fs2Streams[F] with WebSockets].toRoutes).reduceK
}
