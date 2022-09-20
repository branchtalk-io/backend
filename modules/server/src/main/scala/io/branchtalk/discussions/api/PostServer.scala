package io.branchtalk.discussions.api

import cats.data.{ NonEmptyList, NonEmptySet }
import cats.effect.{ Async, Sync }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.PostModels._
import io.branchtalk.discussions.model.{ Channel, Post }
import io.branchtalk.discussions.reads.PostReads
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CommonError, CreationScheduled, ID, Paginated }
import io.branchtalk.users.model.User
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.SortedSet

final class PostServer[F[_]: Async](
  authServices:     AuthServices[F],
  postReads:        PostReads[F],
  postWrites:       PostWrites[F],
  paginationConfig: PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  private val serverOptions: Http4sServerOptions[F] = PostServer.serverOptions[F].apply(logger)

  implicit private val errorHandler: ServerErrorHandler[F, PostError] = PostServer.errorHandler[F].apply(logger)

  private def testOwnership(channelID: ID[Channel], postID: ID[Post], isDeleted: Boolean = false) = postReads
    .requireById(postID, isDeleted)
    .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
    .void

  private def resolveOwnership(channelID: ID[Channel], postID: ID[Post], isDeleted: Boolean = false) = postReads
    .requireById(postID, isDeleted)
    .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
    .map(_.data.authorID)
    .map(userIDApi2Discussions.reverseGet)

  private val newest = PostAPIs.newest.serverLogic[F, Option[User]] { case (channelID, optOffset, optLimit) =>
    val sortBy     = Post.Sorting.Newest
    val offset     = paginationConfig.resolveOffset(optOffset)
    val limit      = paginationConfig.resolveLimit(optLimit)
    val channelIDS = SortedSet(channelID)
    for {
      paginated <- NonEmptySet.fromSet(channelIDS) match {
        case Some(channelIDs) => postReads.paginate(channelIDs, sortBy, offset.nonNegativeLong, limit.positiveInt)
        case None             => Paginated.empty[Post].pure[F]
      }
    } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
  }

  private val hottest = PostAPIs.hottest.serverLogic[F, Option[User]] { channelID =>
    val sortBy     = Post.Sorting.Hottest
    val offset     = paginationConfig.resolveOffset(None)
    val limit      = paginationConfig.resolveLimit(None)
    val channelIDS = SortedSet(channelID)
    for {
      paginated <- NonEmptySet.fromSet(channelIDS) match {
        case Some(channelIDs) => postReads.paginate(channelIDs, sortBy, offset.nonNegativeLong, limit.positiveInt)
        case None             => Paginated.empty[Post].pure[F]
      }
    } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
  }

  private val controversial = PostAPIs.controversial.serverLogic[F, Option[User]] { channelID =>
    val sortBy     = Post.Sorting.Controversial
    val offset     = paginationConfig.resolveOffset(None)
    val limit      = paginationConfig.resolveLimit(None)
    val channelIDS = SortedSet(channelID)
    for {
      paginated <- NonEmptySet.fromSet(channelIDS) match {
        case Some(channelIDs) => postReads.paginate(channelIDs, sortBy, offset.nonNegativeLong, limit.positiveInt)
        case None             => Paginated.empty[Post].pure[F]
      }
    } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit)
  }

  private val create = PostAPIs.create.serverLogic[F, User].withUser { case (user, (channelID, createData)) =>
    val userID = user.id
    val data = createData
      .into[Post.Create]
      .withFieldConst(_.authorID, userIDUsers2Discussions.get(userID))
      .withFieldConst(_.channelID, channelID)
      .transform
    for {
      CreationScheduled(postID) <- postWrites.createPost(data)
    } yield CreatePostResponse(postID)
  }

  private val read = PostAPIs.read.serverLogicWithOwnership[F, Option[User], Unit] { case (channelID, postID) =>
    testOwnership(channelID, postID)
  } { case (_, postID) =>
    for {
      post <- postReads.requireById(postID)
    } yield APIPost.fromDomain(post)
  }

  private val update = PostAPIs.update
    .serverLogicWithOwnership[F, User, UserID] { case (channelID, postID, _) =>
      resolveOwnership(channelID, postID)
    }
    .withUser { case (user, (_, postID, updateData)) =>
      val userID = user.id
      val data = updateData
        .into[Post.Update]
        .withFieldConst(_.id, postID)
        .withFieldConst(_.editorID, userIDUsers2Discussions.get(userID))
        .transform
      for {
        _ <- postWrites.updatePost(data)
      } yield UpdatePostResponse(postID)
    }

  private val delete = PostAPIs.delete
    .serverLogicWithOwnership[F, User, UserID] { case (channelID, postID) =>
      resolveOwnership(channelID, postID)
    }
    .withUser { case (user, (_, postID)) =>
      val userID = user.id
      val data   = Post.Delete(postID, userIDUsers2Discussions.get(userID))
      for {
        _ <- postWrites.deletePost(data)
      } yield DeletePostResponse(postID)
    }

  private val restore = PostAPIs.restore
    .serverLogicWithOwnership[F, User, UserID] { case (channelID, postID) =>
      resolveOwnership(channelID, postID, isDeleted = true)
    }
    .withUser { case (user, (_, postID)) =>
      val userID = user.id
      val data   = Post.Restore(postID, userIDUsers2Discussions.get(userID))
      for {
        _ <- postWrites.restorePost(data)
      } yield RestorePostResponse(postID)
    }

  private val upvote = PostAPIs.upvote
    .serverLogicWithOwnership[F, User, UserID] { case (channelID, postID) =>
      resolveOwnership(channelID, postID)
    }
    .withUser { case (user, (_, postID)) =>
      val userID = user.id
      val data   = Post.Upvote(postID, userIDUsers2Discussions.get(userID))
      for {
        _ <- postWrites.upvotePost(data)
      } yield ()
    }

  private val downvote = PostAPIs.downvote
    .serverLogicWithOwnership[F, User, UserID] { case (channelID, postID) =>
      resolveOwnership(channelID, postID)
    }
    .withUser { case (user, (_, postID)) =>
      val userID = user.id
      val data   = Post.Downvote(postID, userIDUsers2Discussions.get(userID))
      for {
        _ <- postWrites.downvotePost(data)
      } yield ()
    }

  private val revokeVote = PostAPIs.revokeVote
    .serverLogicWithOwnership[F, User, UserID] { case (channelID, postID) =>
      resolveOwnership(channelID, postID)
    }
    .withUser { case (user, (_, postID)) =>
      val userID = user.id
      val data   = Post.RevokeVote(postID, userIDUsers2Discussions.get(userID))
      for {
        _ <- postWrites.revokePostVote(data)
      } yield ()
    }

  def endpoints: NonEmptyList[ServerEndpoint[Any, F]] = NonEmptyList.of[ServerEndpoint[Any, F]](
    newest,
    hottest,
    controversial,
    create,
    read,
    update,
    delete,
    restore,
    upvote,
    downvote,
    revokeVote
  )

  val routes: HttpRoutes[F] = Http4sServerInterpreter(serverOptions).toRoutes(endpoints.toList)
}
object PostServer {

  def serverOptions[F[_]: Sync]: Logger => Http4sServerOptions[F] = ServerOptions.create[F, PostError](
    _,
    ServerOptions.ErrorHandler[PostError](
      () => PostError.ValidationFailed(NonEmptyList.one("Data missing")),
      () => PostError.ValidationFailed(NonEmptyList.one("Multiple errors")),
      (msg, _) => PostError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
      (expected, actual) => PostError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
      errors =>
        PostError.ValidationFailed(
          NonEmptyList
            .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
            .getOrElse(NonEmptyList.one("Validation failed"))
        )
    )
  )

  def errorHandler[F[_]: Sync]: Logger => ServerErrorHandler[F, PostError] =
    ServerErrorHandler.handleCommonErrors[F, PostError] {
      case CommonError.InvalidCredentials(_) =>
        PostError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        PostError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        PostError.NotFound(show"$what with id=$id could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        PostError.NotFound(show"Parent $what with id=$id could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        PostError.ValidationFailed(errors)
    }
}
