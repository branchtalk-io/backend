package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import cats.effect.{ Concurrent, ContextShift, Sync, Timer }
import com.typesafe.scalalogging.Logger
import io.branchtalk.api._
import io.branchtalk.auth._
import io.branchtalk.configs.PaginationConfig
import io.branchtalk.discussions.api.CommentModels._
import io.branchtalk.discussions.model.{ Channel, Comment, Post }
import io.branchtalk.discussions.reads.{ CommentReads, PostReads }
import io.branchtalk.discussions.writes.CommentWrites
import io.branchtalk.mappings._
import io.branchtalk.shared.model.{ CommonError, CreationScheduled, ID }
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._
import sttp.tapir.server.ServerEndpoint

final class CommentServer[F[_]: Sync: ContextShift: Concurrent: Timer](
  authServices:     AuthServices[F],
  postReads:        PostReads[F],
  commentReads:     CommentReads[F],
  commentWrites:    CommentWrites[F],
  paginationConfig: PaginationConfig
) {

  implicit private val as: AuthServices[F] = authServices

  private val logger = Logger(getClass)

  implicit private val serverOptions: Http4sServerOptions[F] = CommentServer.serverOptions[F].apply(logger)

  implicit private val errorHandler: ServerErrorHandler[F, CommentError] = CommentServer.errorHandler[F].apply(logger)

  private def testOwnership(channelID: ID[Channel], postID: ID[Post]) = postReads
    .requireById(postID)
    .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
    .void

  private def testOwnership(
    channelID: ID[Channel],
    postID:    ID[Post],
    commentID: ID[Comment],
    isDeleted: Boolean = false
  ) = postReads
    .requireById(postID)
    .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
    .void >> commentReads
    .requireById(commentID, isDeleted)
    .flatTap(comment => Sync[F].delay(assert(comment.data.postID === postID, "Comment should belong to Post")))
    .void

  private def resolveOwnership(
    channelID: ID[Channel],
    postID:    ID[Post],
    commentID: ID[Comment],
    isDeleted: Boolean = false
  ) = postReads
    .requireById(postID)
    .flatTap(post => Sync[F].delay(assert(post.data.channelID === channelID, "Post should belong to Channel")))
    .void >> commentReads
    .requireById(commentID, isDeleted)
    .flatTap(comment => Sync[F].delay(assert(comment.data.postID === postID, "Comment should belong to Post")))
    .map(_.data.authorID)
    .map(userIDApi2Discussions.reverseGet)

  private val newest = CommentAPIs.newest
    .serverLogicWithOwnership[F, Unit]
    .apply { case (_, channelID, postID, _, _, _) => testOwnership(channelID, postID) } {
      case ((_, _), _, postID, optOffset, optLimit, optReply) =>
        val sortBy = Comment.Sorting.Newest
        val offset = paginationConfig.resolveOffset(optOffset)
        val limit  = paginationConfig.resolveLimit(optLimit)
        for {
          paginated <- commentReads.paginate(postID, optReply, sortBy, offset.nonNegativeLong, limit.positiveInt)
        } yield Pagination.fromPaginated(paginated.map(APIComment.fromDomain), offset, limit)
    }

  private val hottest = CommentAPIs.hottest
    .serverLogicWithOwnership[F, Unit]
    .apply { case (_, channelID, postID, _) => testOwnership(channelID, postID) } {
      case ((_, _), _, postID, optReply) =>
        val sortBy = Comment.Sorting.Hottest
        val offset = paginationConfig.resolveOffset(None)
        val limit  = paginationConfig.resolveLimit(None)
        for {
          paginated <- commentReads.paginate(postID, optReply, sortBy, offset.nonNegativeLong, limit.positiveInt)
        } yield Pagination.fromPaginated(paginated.map(APIComment.fromDomain), offset, limit)
    }

  private val controversial = CommentAPIs.controversial
    .serverLogicWithOwnership[F, Unit]
    .apply { case (_, channelID, postID, _) => testOwnership(channelID, postID) } {
      case ((_, _), _, postID, optReply) =>
        val sortBy = Comment.Sorting.Controversial
        val offset = paginationConfig.resolveOffset(None)
        val limit  = paginationConfig.resolveLimit(None)
        for {
          paginated <- commentReads.paginate(postID, optReply, sortBy, offset.nonNegativeLong, limit.positiveInt)
        } yield Pagination.fromPaginated(paginated.map(APIComment.fromDomain), offset, limit)
    }

  private val create = CommentAPIs.create
    .serverLogicWithOwnership[F, Unit]
    .apply { case (_, channelID, postID, _) => testOwnership(channelID, postID) } {
      case ((user, _), _, postID, createData) =>
        val userID = user.id
        val data = createData
          .into[Comment.Create]
          .withFieldConst(_.authorID, userIDUsers2Discussions.get(userID))
          .withFieldConst(_.postID, postID)
          .transform
        for {
          CreationScheduled(commentID) <- commentWrites.createComment(data)
        } yield CreateCommentResponse(commentID)
    }

  private val read = CommentAPIs.read
    .serverLogicWithOwnership[F, Unit]
    .apply { case (_, channelID, postID, commentID) => testOwnership(channelID, postID, commentID) } {
      case ((_, _), _, _, commentID) =>
        for {
          comment <- commentReads.requireById(commentID)
        } yield APIComment.fromDomain(comment)
    }

  private val update = CommentAPIs.update
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, channelID, postID, commentID, _) => resolveOwnership(channelID, postID, commentID) } {
      case ((user, _), _, _, commentID, updateData) =>
        val userID = user.id
        val data = updateData
          .into[Comment.Update]
          .withFieldConst(_.id, commentID)
          .withFieldConst(_.editorID, userIDUsers2Discussions.get(userID))
          .transform
        for {
          _ <- commentWrites.updateComment(data)
        } yield UpdateCommentResponse(commentID)
    }

  private val delete = CommentAPIs.delete
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, channelID, postID, commentID) => resolveOwnership(channelID, postID, commentID) } {
      case ((user, _), _, _, commentID) =>
        val userID = user.id
        val data   = Comment.Delete(commentID, userIDUsers2Discussions.get(userID))
        for {
          _ <- commentWrites.deleteComment(data)
        } yield DeleteCommentResponse(commentID)
    }

  private val restore = CommentAPIs.restore
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, channelID, postID, commentID) =>
      resolveOwnership(channelID, postID, commentID, isDeleted = true)
    } { case ((user, _), _, _, commentID) =>
      val userID = user.id
      val data   = Comment.Restore(commentID, userIDUsers2Discussions.get(userID))
      for {
        _ <- commentWrites.restoreComment(data)
      } yield RestoreCommentResponse(commentID)
    }

  private val upvote = CommentAPIs.upvote
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, channelID, postID, commentID) =>
      resolveOwnership(channelID, postID, commentID)
    } { case ((user, _), _, _, commentID) =>
      val userID = user.id
      val data   = Comment.Upvote(commentID, userIDUsers2Discussions.get(userID))
      for {
        _ <- commentWrites.upvoteComment(data)
      } yield ()
    }

  private val downvote = CommentAPIs.downvote
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, channelID, postID, commentID) =>
      resolveOwnership(channelID, postID, commentID)
    } { case ((user, _), _, _, commentID) =>
      val userID = user.id
      val data   = Comment.Downvote(commentID, userIDUsers2Discussions.get(userID))
      for {
        _ <- commentWrites.downvoteComment(data)
      } yield ()
    }

  private val revokeVote = CommentAPIs.revokeVote
    .serverLogicWithOwnership[F, UserID]
    .apply { case (_, channelID, postID, commentID) =>
      resolveOwnership(channelID, postID, commentID)
    } { case ((user, _), _, _, commentID) =>
      val userID = user.id
      val data   = Comment.RevokeVote(commentID, userIDUsers2Discussions.get(userID))
      for {
        _ <- commentWrites.revokeCommentVote(data)
      } yield ()
    }

  def endpoints: NonEmptyList[ServerEndpoint[_, CommentError, _, Any, F]] = NonEmptyList.of(
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

  val routes: HttpRoutes[F] = endpoints.map(Http4sServerInterpreter.toRoutes(_)).reduceK
}
object CommentServer {

  def serverOptions[F[_]: Sync: ContextShift]: Logger => Http4sServerOptions[F] = ServerOptions.create[F, CommentError](
    _,
    ServerOptions.ErrorHandler[CommentError](
      () => CommentError.ValidationFailed(NonEmptyList.one("Data missing")),
      () => CommentError.ValidationFailed(NonEmptyList.one("Multiple errors")),
      (msg, _) => CommentError.ValidationFailed(NonEmptyList.one(s"Error happened: ${msg}")),
      (expected, actual) => CommentError.ValidationFailed(NonEmptyList.one(s"Expected: $expected, actual: $actual")),
      errors =>
        CommentError.ValidationFailed(
          NonEmptyList
            .fromList(errors.map(e => s"Invalid value at ${e.path.map(_.encodedName).mkString(".")}"))
            .getOrElse(NonEmptyList.one("Validation failed"))
        )
    )
  )

  def errorHandler[F[_]: Sync]: Logger => ServerErrorHandler[F, CommentError] =
    ServerErrorHandler.handleCommonErrors[F, CommentError] {
      case CommonError.InvalidCredentials(_) =>
        CommentError.BadCredentials("Invalid credentials")
      case CommonError.InsufficientPermissions(msg, _) =>
        CommentError.NoPermission(msg)
      case CommonError.NotFound(what, id, _) =>
        CommentError.NotFound(show"$what with id=$id could not be found")
      case CommonError.ParentNotExist(what, id, _) =>
        CommentError.NotFound(show"Parent $what with id=$id could not be found")
      case CommonError.ValidationFailed(errors, _) =>
        CommentError.ValidationFailed(errors)
    }
}
