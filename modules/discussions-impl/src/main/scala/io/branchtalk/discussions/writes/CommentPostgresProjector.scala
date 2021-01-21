package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import doobie.Transactor
import fs2.Stream
import io.branchtalk.discussions.events.{ CommentEvent, DiscussionEvent }
import io.branchtalk.discussions.infrastructure.DoobieExtensions._
import io.branchtalk.discussions.model.{ Comment, User, Vote }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ ID, UUID }

final class CommentPostgresProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, DiscussionEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionEvent.ForComment(event) =>
      event
    }.evalMap[F, (UUID, CommentEvent)] {
      case event: CommentEvent.Created     => toCreate(event).widen
      case event: CommentEvent.Updated     => toUpdate(event).widen
      case event: CommentEvent.Deleted     => toDelete(event).widen
      case event: CommentEvent.Restored    => toRestore(event).widen
      case event: CommentEvent.Upvoted     => toUpvote(event).widen
      case event: CommentEvent.Downvoted   => toDownvote(event).widen
      case event: CommentEvent.VoteRevoked => toRevokeVote(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForComment(value)
    }.handleErrorWith { error =>
      logger.error("Comment event processing failed", error)
      Stream.empty
    }

  def toCreate(event: CommentEvent.Created): F[(UUID, CommentEvent.Created)] =
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
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toUpdate(event: CommentEvent.Updated): F[(UUID, CommentEvent.Updated)] =
    withCorrelationID(event.correlationID) {
      (NonEmptyList.fromList(
        List(
          event.newContent.toUpdateFragment(fr"content")
        ).flatten
      ) match {
        case Some(updates) =>
          (fr"UPDATE comments SET" ++
            (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
            fr"WHERE id = ${event.id}").update.run.void
        case None =>
          Sync[ConnectionIO].delay(
            logger.warn(show"Comment update ignored as it doesn't contain any modification:\n$event")
          )
      }).as(event.id.uuid -> event).transact(transactor)
    }

  def toDelete(event: CommentEvent.Deleted): F[(UUID, CommentEvent.Deleted)] =
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
             |""".stripMargin.update.run).as(event.id.uuid -> event).transact(transactor)
    }

  def toRestore(event: CommentEvent.Restored): F[(UUID, CommentEvent.Restored)] =
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
             |""".stripMargin.update.run).as(event.id.uuid -> event).transact(transactor)
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

  def toUpvote(event: CommentEvent.Upvoted): F[(UUID, CommentEvent.Upvoted)] =
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
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toDownvote(event: CommentEvent.Downvoted): F[(UUID, CommentEvent.Downvoted)] =
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
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toRevokeVote(event: CommentEvent.VoteRevoked): F[(UUID, CommentEvent.VoteRevoked)] =
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
        .as(event.id.uuid -> event)
        .transact(transactor)
    }
}
