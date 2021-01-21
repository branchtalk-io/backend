package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import doobie.Transactor
import fs2.Stream
import io.branchtalk.discussions.events.{ DiscussionEvent, PostEvent }
import io.branchtalk.discussions.infrastructure.DoobieExtensions._
import io.branchtalk.discussions.model.{ Post, User, Vote }
import io.branchtalk.logging.MDC
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ ID, UUID }

final class PostPostgresProjector[F[_]: Sync: MDC](transactor: Transactor[F])
    extends Projector[F, DiscussionEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionEvent.ForPost(event) =>
      event
    }.evalMap[F, (UUID, PostEvent)] {
      case event: PostEvent.Created     => toCreate(event).widen
      case event: PostEvent.Updated     => toUpdate(event).widen
      case event: PostEvent.Deleted     => toDelete(event).widen
      case event: PostEvent.Restored    => toRestore(event).widen
      case event: PostEvent.Upvoted     => toUpvote(event).widen
      case event: PostEvent.Downvoted   => toDownvote(event).widen
      case event: PostEvent.VoteRevoked => toRevokeVote(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForPost(value)
    }.handleErrorWith { error =>
      logger.error("Post event processing failed", error)
      Stream.empty
    }

  def toCreate(event: PostEvent.Created): F[(UUID, PostEvent.Created)] =
    withCorrelationID(event.correlationID) {
      val Post.Content.Tupled(contentType, contentRaw) = event.content
      sql"""INSERT INTO posts (
           |  id,
           |  author_id,
           |  channel_id,
           |  url_title,
           |  title,
           |  content_type,
           |  content_raw,
           |  created_at
           |)
           |VALUES (
           |  ${event.id},
           |  ${event.authorID},
           |  ${event.channelID},
           |  ${event.urlTitle},
           |  ${event.title},
           |  ${contentType},
           |  ${contentRaw},
           |  ${event.createdAt}
           |)
           |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run.as(event.id.uuid -> event).transact(transactor)
    }

  def toUpdate(event: PostEvent.Updated): F[(UUID, PostEvent.Updated)] =
    withCorrelationID(event.correlationID) {
      val contentTupled = event.newContent.map(Post.Content.Tupled.unpack)
      (NonEmptyList.fromList(
        List(
          event.newUrlTitle.toUpdateFragment(fr"url_title"),
          event.newTitle.toUpdateFragment(fr"title"),
          contentTupled.map(_._1).toUpdateFragment(fr"content_type"),
          contentTupled.map(_._2).toUpdateFragment(fr"content_raw")
        ).flatten
      ) match {
        case Some(updates) =>
          (fr"UPDATE posts SET" ++
            (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
            fr"WHERE id = ${event.id}").update.run.void
        case None =>
          Sync[ConnectionIO].delay(
            logger.warn(show"Post update ignored as it doesn't contain any modification:\n$event")
          )
      }).as(event.id.uuid -> event).transact(transactor)
    }

  def toDelete(event: PostEvent.Deleted): F[(UUID, PostEvent.Deleted)] =
    withCorrelationID(event.correlationID) {
      sql"UPDATE posts SET deleted = TRUE WHERE id = ${event.id}".update.run
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toRestore(event: PostEvent.Restored): F[(UUID, PostEvent.Restored)] =
    withCorrelationID(event.correlationID) {
      sql"UPDATE posts SET deleted = FALSE WHERE id = ${event.id}".update.run
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  private def fetchVote(postID: ID[Post], voterID: ID[User]) =
    sql"SELECT vote FROM post_votes WHERE post_id = ${postID} AND voter_id = ${voterID}".query[Vote.Type].option

  private def updatePostVotes(postID: ID[Post], upvotes: Fragment, downvotes: Fragment) =
    (fr"WITH nw AS (SELECT" ++ upvotes ++ fr"AS upvotes,"
      ++ downvotes ++ fr"AS downvotes FROM posts WHERE id = ${postID})" ++
      fr"""
          |UPDATE posts
          |SET upvotes_nr          = nw.upvotes,
          |    downvotes_nr        = nw.downvotes,
          |    total_score         = nw.upvotes - nw.downvotes,
          |    controversial_score = LEAST(nw.upvotes, nw.downvotes)
          |FROM nw
          |WHERE id = ${postID}""".stripMargin).update.run

  def toUpvote(event: PostEvent.Upvoted): F[(UUID, PostEvent.Upvoted)] =
    withCorrelationID(event.correlationID) {
      fetchVote(event.id, event.voterID)
        .flatMap {
          case Some(Vote.Type.Upvote) =>
            // do nothing - upvote already exists
            ().pure[ConnectionIO]
          case Some(Vote.Type.Downvote) =>
            // swap downvote->upvote
            sql"""UPDATE post_votes
                 |SET vote = ${Vote.Type.upvote}
                 |WHERE post_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updatePostVotes(event.id, fr"upvotes_nr + 1", fr"downvotes_nr - 1").void
          case None =>
            // create new upvote
            sql"""INSERT INTO post_votes (
                 |  post_id,
                 |  voter_id,
                 |  vote
                 |) VALUES (
                 |  ${event.id},
                 |  ${event.voterID},
                 |  ${Vote.Type.upvote}
                 |)""".stripMargin.update.run >>
              updatePostVotes(event.id, fr"upvotes_nr + 1", fr"downvotes_nr").void
        }
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toDownvote(event: PostEvent.Downvoted): F[(UUID, PostEvent.Downvoted)] =
    withCorrelationID(event.correlationID) {
      fetchVote(event.id, event.voterID)
        .flatMap {
          case Some(Vote.Type.Upvote) =>
            // swap upvote->downvote
            sql"""UPDATE post_votes
                 |SET vote = ${Vote.Type.downvote}
                 |WHERE post_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updatePostVotes(event.id, fr"upvotes_nr - 1", fr"downvotes_nr + 1").void
          case Some(Vote.Type.Downvote) =>
            // do nothing - downvote already exists
            ().pure[ConnectionIO]
          case None =>
            // create new downvote
            sql"""INSERT INTO post_votes (
                 |  post_id,
                 |  voter_id,
                 |  vote
                 |) VALUES (
                 |  ${event.id},
                 |  ${event.voterID},
                 |  ${Vote.Type.downvote}
                 |)""".stripMargin.update.run >>
              updatePostVotes(event.id, fr"upvotes_nr", fr"downvotes_nr + 1").void
        }
        .as(event.id.uuid -> event)
        .transact(transactor)
    }

  def toRevokeVote(event: PostEvent.VoteRevoked): F[(UUID, PostEvent.VoteRevoked)] =
    withCorrelationID(event.correlationID) {
      fetchVote(event.id, event.voterID)
        .flatMap {
          case Some(Vote.Type.Upvote) =>
            // delete upvote
            sql"""DELETE FROM post_votes
                 |WHERE post_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updatePostVotes(event.id, fr"upvotes_nr - 1", fr"downvotes_nr").void
          case Some(Vote.Type.Downvote) =>
            // delete downvote
            sql"""DELETE FROM post_votes
                 |WHERE post_id = ${event.id}
                 |  AND voter_id = ${event.voterID}""".stripMargin.update.run >>
              updatePostVotes(event.id, fr"upvotes_nr", fr"downvotes_nr - 1").void
          case None =>
            // do nothing - vote doesn't exist
            ().pure[ConnectionIO]
        }
        .as(event.id.uuid -> event)
        .transact(transactor)
    }
}
