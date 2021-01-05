package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import doobie.Transactor
import fs2.Stream
import io.branchtalk.discussions.events.{ CommentCommandEvent, CommentEvent, DiscussionEvent, DiscussionsCommandEvent }
import io.branchtalk.discussions.infrastructure.DoobieExtensions._
import io.branchtalk.discussions.model.{ Comment, User, Vote }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ ID, UUID }
import io.scalaland.chimney.dsl._

final class CommentProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionsCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionsCommandEvent.ForComment(event) =>
      event
    }.evalMap[F, (UUID, CommentEvent)] {
      case event: CommentCommandEvent.Create     => toCreate(event).widen
      case event: CommentCommandEvent.Update     => toUpdate(event).widen
      case event: CommentCommandEvent.Delete     => toDelete(event).widen
      case event: CommentCommandEvent.Restore    => toRestore(event).widen
      case event: CommentCommandEvent.Upvote     => toUpvote(event).widen
      case event: CommentCommandEvent.Downvote   => toDownvote(event).widen
      case event: CommentCommandEvent.RevokeVote => toRevokeVote(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForComment(value)
    }.handleErrorWith { error =>
      logger.error("Post event processing failed", error)
      Stream.empty
    }

  def toCreate(event: CommentCommandEvent.Create): F[(UUID, CommentEvent.Created)] =
    withCorrelationID(event.correlationID) {
      event.replyTo
        .fold(0.pure[ConnectionIO]) { replyId =>
          sql"""SELECT nesting_level + 1
               |FROM comments
               |WHERE id = ${replyId}""".stripMargin.query[Int].option.map(_.getOrElse(0))
        }
        .flatMap { nestingLevel =>
          sql"""INSERT INTO comments (
               |  id,
               |  author_id,
               |  channel_id,
               |  post_id,
               |  content,
               |  reply_to,
               |  nesting_level,
               |  created_at
               |)
               |VALUES (
               |  ${event.id},
               |  ${event.authorID},
               |  ${event.channelID},
               |  ${event.postID},
               |  ${event.content},
               |  ${event.replyTo},
               |  ${nestingLevel},
               |  ${event.createdAt}
               |)
               |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run >>
            sql"""UPDATE posts
                 |SET comments_nr = comments_nr + 1
                 |WHERE id = ${event.postID}
                 |""".stripMargin.update.run >>
            sql"""UPDATE comments
                 |SET replies_nr = replies_nr + 1
                 |WHERE id = ${event.replyTo}
                 |""".stripMargin.update.run
        }
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[CommentEvent.Created])
    }

  def toUpdate(event: CommentCommandEvent.Update): F[(UUID, CommentEvent.Updated)] =
    withCorrelationID(event.correlationID) {
      (NonEmptyList.fromList(
        List(
          event.newContent.toUpdateFragment(fr"content")
        ).flatten
      ) match {
        case Some(updates) =>
          (fr"UPDATE comments SET" ++
            (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
            fr"WHERE id = ${event.id}").update.run.transact(transactor).void
        case None =>
          Sync[F].delay(logger.warn(s"Comment update ignored as it doesn't contain any modification:\n${event.show}"))
      }).as(event.id.uuid -> event.transformInto[CommentEvent.Updated])
    }

  def toDelete(event: CommentCommandEvent.Delete): F[(UUID, CommentEvent.Deleted)] =
    withCorrelationID(event.correlationID) {
      (sql"UPDATE comments SET deleted = TRUE WHERE id = ${event.id}".update.run >>
        sql"""UPDATE posts
             |SET comments_nr = comments_nr - 1
             |FROM (SELECT post_id FROM comments WHERE id = ${event.id}) as subquery
             |WHERE id = subquery.post_id
             |""".stripMargin.update.run >>
        sql"""UPDATE comments
             |SET replies_nr = replies_nr - 1
             |FROM (SELECT reply_to FROM comments WHERE id = ${event.id}) as subquery
             |WHERE id = subquery.reply_to
             |""".stripMargin.update.run)
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[CommentEvent.Deleted])
    }

  def toRestore(event: CommentCommandEvent.Restore): F[(UUID, CommentEvent.Restored)] =
    withCorrelationID(event.correlationID) {
      (sql"UPDATE comments SET deleted = FALSE WHERE id = ${event.id}".update.run >>
        sql"""UPDATE posts
             |SET comments_nr = comments_nr + 1
             |FROM (SELECT post_id FROM comments WHERE id = ${event.id}) as subquery
             |WHERE id = subquery.post_id
             |""".stripMargin.update.run >>
        sql"""UPDATE comments
             |SET replies_nr = replies_nr + 1
             |FROM (SELECT reply_to FROM comments WHERE id = ${event.id}) as subquery
             |WHERE id = subquery.reply_to
             |""".stripMargin.update.run)
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[CommentEvent.Restored])
    }

  private def fetchVote(commentID: ID[Comment], voterID: ID[User]) =
    sql"SELECT vote FROM comment_votes WHERE comment_id = ${commentID} AND voter_id = ${voterID}"
      .query[Vote.Type]
      .option

  private def updateCommentVotes(commentID: ID[Comment], upvotes: Fragment, downvotes: Fragment) =
    (fr"WITH nw AS (SELECT" ++ upvotes ++ fr"AS upvotes,"
      ++ downvotes ++ fr"AS downvotes FROM comments WHERE id = ${commentID})" ++
      fr"""
          |UPDATE comments
          |SET upvotes_nr          = nw.upvotes,
          |    downvotes_nr        = nw.downvotes,
          |    total_score         = nw.upvotes - nw.downvotes,
          |    controversial_score = LEAST(nw.upvotes, nw.downvotes)
          |FROM nw
          |WHERE id = ${commentID}""".stripMargin).update.run

  def toUpvote(event: CommentCommandEvent.Upvote): F[(UUID, CommentEvent.Upvoted)] =
    withCorrelationID(event.correlationID) {
      fetchVote(event.id, event.voterID)
        .flatMap {
          case Some(Vote.Type.Upvote) =>
            // do nothing - upvote already exists
            ().pure[ConnectionIO]
          case Some(Vote.Type.Downvote) =>
            // swap downvote->upvote
            sql"""UPDATE comment_votes
                 |SET vote = ${Vote.Type.upvote}
                 |WHERE comment_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updateCommentVotes(event.id, fr"upvotes_nr + 1", fr"downvotes_nr - 1").void
          case None =>
            // create new upvote
            sql"""INSERT INTO comment_votes (
                 |  comment_id,
                 |  voter_id,
                 |  vote
                 |) VALUES (
                 |  ${event.id},
                 |  ${event.voterID},
                 |  ${Vote.Type.upvote}
                 |)""".stripMargin.update.run >>
              updateCommentVotes(event.id, fr"upvotes_nr + 1", fr"downvotes_nr").void
        }
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[CommentEvent.Upvoted])
    }

  def toDownvote(event: CommentCommandEvent.Downvote): F[(UUID, CommentEvent.Downvoted)] =
    withCorrelationID(event.correlationID) {
      fetchVote(event.id, event.voterID)
        .flatMap {
          case Some(Vote.Type.Upvote) =>
            // swap upvote->downvote
            sql"""UPDATE comment_votes
                 |SET vote = ${Vote.Type.downvote}
                 |WHERE comment_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updateCommentVotes(event.id, fr"upvotes_nr - 1", fr"downvotes_nr + 1").void
          case Some(Vote.Type.Downvote) =>
            // do nothing - downvote already exists
            ().pure[ConnectionIO]
          case None =>
            // create new downvote
            sql"""INSERT INTO comment_votes (
                 |  comment_id,
                 |  voter_id,
                 |  vote
                 |) VALUES (
                 |  ${event.id},
                 |  ${event.voterID},
                 |  ${Vote.Type.downvote}
                 |)""".stripMargin.update.run >>
              updateCommentVotes(event.id, fr"upvotes_nr", fr"downvotes_nr + 1").void
        }
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[CommentEvent.Downvoted])
    }

  def toRevokeVote(event: CommentCommandEvent.RevokeVote): F[(UUID, CommentEvent.VoteRevoked)] =
    withCorrelationID(event.correlationID) {
      fetchVote(event.id, event.voterID)
        .flatMap {
          case Some(Vote.Type.Upvote) =>
            // delete upvote
            sql"""DELETE FROM comment_votes
                 |WHERE comment_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updateCommentVotes(event.id, fr"upvotes_nr - 1", fr"downvotes_nr").void
          case Some(Vote.Type.Downvote) =>
            // delete downvote
            sql"""DELETE FROM comment_votes
                 |WHERE comment_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updateCommentVotes(event.id, fr"upvotes_nr", fr"downvotes_nr - 1").void
          case None =>
            // do nothing - vote doesn't exist
            ().pure[ConnectionIO]
        }
        .transact(transactor)
        .as(event.id.uuid -> event.transformInto[CommentEvent.VoteRevoked])
    }
}
