package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import doobie.Transactor
import fs2.Stream
import io.branchtalk.discussions.events.{ DiscussionEvent, DiscussionsCommandEvent, PostCommandEvent, PostEvent }
import io.branchtalk.discussions.infrastructure.DoobieExtensions._
import io.branchtalk.discussions.model.{ Post, User, Vote }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ ID, UUID }
import io.scalaland.chimney.dsl._

final class PostProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, DiscussionsCommandEvent, (UUID, DiscussionEvent)] {

  private val logger = Logger(getClass)

  implicit private val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, DiscussionsCommandEvent]): Stream[F, (UUID, DiscussionEvent)] =
    in.collect { case DiscussionsCommandEvent.ForPost(event) =>
      event
    }.evalMap[F, (UUID, PostEvent)] {
      case event: PostCommandEvent.Create     => toCreate(event).widen
      case event: PostCommandEvent.Update     => toUpdate(event).widen
      case event: PostCommandEvent.Delete     => toDelete(event).widen
      case event: PostCommandEvent.Restore    => toRestore(event).widen
      case event: PostCommandEvent.Upvote     => toUpvote(event).widen
      case event: PostCommandEvent.Downvote   => toDownvote(event).widen
      case event: PostCommandEvent.RevokeVote => toRevokeVote(event).widen
    }.map { case (key, value) =>
      key -> DiscussionEvent.ForPost(value)
    }.handleErrorWith { error =>
      logger.error("Post event processing failed", error)
      Stream.empty
    }

  def toCreate(event: PostCommandEvent.Create): F[(UUID, PostEvent.Created)] = {
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
         |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run
      .transact(transactor)
      .as(event.id.uuid -> event.transformInto[PostEvent.Created])
  }

  def toUpdate(event: PostCommandEvent.Update): F[(UUID, PostEvent.Updated)] = {
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
          fr"WHERE id = ${event.id}").update.run.transact(transactor).void
      case None =>
        Sync[F].delay(logger.warn(s"Post update ignored as it doesn't contain any modification:\n${event.show}"))
    }).as(event.id.uuid -> event.transformInto[PostEvent.Updated])
  }

  def toDelete(event: PostCommandEvent.Delete): F[(UUID, PostEvent.Deleted)] =
    sql"UPDATE posts SET deleted = TRUE WHERE id = ${event.id}".update.run
      .transact(transactor)
      .as(event.id.uuid -> event.transformInto[PostEvent.Deleted])

  def toRestore(event: PostCommandEvent.Restore): F[(UUID, PostEvent.Restored)] =
    sql"UPDATE posts SET deleted = FALSE WHERE id = ${event.id}".update.run
      .transact(transactor)
      .as(event.id.uuid -> event.transformInto[PostEvent.Restored])

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

  def toUpvote(event: PostCommandEvent.Upvote): F[(UUID, PostEvent.Upvoted)] =
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
      .transact(transactor)
      .as(event.id.uuid -> event.transformInto[PostEvent.Upvoted])

  def toDownvote(event: PostCommandEvent.Downvote): F[(UUID, PostEvent.Downvoted)] =
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
      .transact(transactor)
      .as(event.id.uuid -> event.transformInto[PostEvent.Downvoted])

  def toRevokeVote(event: PostCommandEvent.RevokeVote): F[(UUID, PostEvent.VoteRevoked)] =
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
      .transact(transactor)
      .as(event.id.uuid -> event.transformInto[PostEvent.VoteRevoked])
}
