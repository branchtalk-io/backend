package io.branchtalk.discussions.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import doobie.Transactor
import fs2.Stream
import io.branchtalk.discussions.events.{ CommentCommandEvent, CommentEvent, DiscussionEvent, DiscussionsCommandEvent }
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.UUID
import io.scalaland.chimney.dsl._

final class CommentProjector[F[_]: Sync](transactor: Transactor[F])
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
      .transact(transactor) >>
      (event.id.uuid -> event.transformInto[CommentEvent.Created]).pure[F]

  def toUpdate(event: CommentCommandEvent.Update): F[(UUID, CommentEvent.Updated)] =
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
    }) >>
      (event.id.uuid -> event.transformInto[CommentEvent.Updated]).pure[F]

  def toDelete(event: CommentCommandEvent.Delete): F[(UUID, CommentEvent.Deleted)] =
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
           |""".stripMargin.update.run).transact(transactor) >>
      (event.id.uuid -> event.transformInto[CommentEvent.Deleted]).pure[F]

  def toRestore(event: CommentCommandEvent.Restore): F[(UUID, CommentEvent.Restored)] =
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
           |""".stripMargin.update.run).transact(transactor) >>
      (event.id.uuid -> event.transformInto[CommentEvent.Restored]).pure[F]

  def toUpvote(event: CommentCommandEvent.Upvote): F[(UUID, CommentEvent.Upvoted)] = ???

  def toDownvote(event: CommentCommandEvent.Downvote): F[(UUID, CommentEvent.Downvoted)] = ???

  def toRevokeVote(event: CommentCommandEvent.RevokeVote): F[(UUID, CommentEvent.VoteRevoked)] = ???
}
