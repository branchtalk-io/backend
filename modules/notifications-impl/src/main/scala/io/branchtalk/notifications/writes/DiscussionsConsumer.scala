package io.branchtalk.notifications.writes

import cats.effect.Sync
import fs2.Stream
import io.branchtalk.discussions.events.{ CommentEvent, DiscussionEvent, PostEvent }
import io.branchtalk.notifications.events.NotificationCommandEvent
import io.branchtalk.notifications.model.{ Comment, Notification, Post, User }
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ CreationTime, ID, UUID }
import com.typesafe.scalalogging.Logger
import org.typelevel.doobie.Transactor

// Consumes discussion events and produces notification command events.
// - When a comment is created, notify the post author (PostReply).
// - When a comment is created as a reply to another comment, notify that comment's author (CommentReply).
// - NewPostInChannel fan-out is a TODO: resolving channel subscribers requires querying the subscriptions table,
//   which lives in the discussions domain. A cross-domain read is possible but deferred for now.
final class DiscussionsConsumer[F[_]: Sync](transactor: Transactor[F])(using UUID.Generator)
    extends Projector[F, DiscussionEvent, (UUID, NotificationCommandEvent)] {

  private val logger = Logger(getClass)

  override def apply(in: Stream[F, DiscussionEvent]): Stream[F, (UUID, NotificationCommandEvent)] =
    in.flatMap {
      case DiscussionEvent.ForComment(event: CommentEvent.Created) =>
        Stream.evalSeq(handleCommentCreated(event))
      case _ =>
        Stream.empty
    }.handleErrorWith { error =>
      logger.error("Notifications DiscussionsConsumer processing failed", error)
      Stream.empty
    }

  private def handleCommentCreated(event: CommentEvent.Created): F[List[(UUID, NotificationCommandEvent)]] = {
    val commentAuthorID = ID[User](event.authorID.unwrap)

    // Notify the post author about the comment (PostReply), unless the commenter IS the post author.
    val postReplyNotification: F[Option[(UUID, NotificationCommandEvent)]] =
      sql"""SELECT author_id FROM posts WHERE id = ${event.postID}"""
        .queryWithLabel[UUID](show"Get post author for Notification, Post=${event.postID}")
        .option
        .transact(transactor)
        .flatMap {
          case Some(postAuthorUUID) if postAuthorUUID =!= event.authorID.unwrap =>
            for {
              notifID <- ID.create[F, io.branchtalk.notifications.model.Notification]
              now <- CreationTime.now[F]
              correlationID = event.correlationID
            } yield Some(
              notifID.unwrap -> NotificationCommandEvent.Create(
                id = notifID,
                recipientID = ID[User](postAuthorUUID),
                kind = Notification.Kind.PostReply,
                sourcePostID = Some(ID[Post](event.postID.unwrap)),
                sourceCommentID = Some(ID[Comment](event.id.unwrap)),
                sourceUserID = Some(commentAuthorID),
                message = Notification.Message.unsafeMake("Someone replied to your post"),
                createdAt = now,
                correlationID = correlationID
              )
            )
          case _ => none[(UUID, NotificationCommandEvent)].pure[F]
        }

    // If this comment is a reply to another comment, notify that comment's author (CommentReply),
    // unless the replier IS that comment's author.
    val commentReplyNotification: F[Option[(UUID, NotificationCommandEvent)]] =
      event.replyTo match {
        case Some(parentCommentID) =>
          sql"""SELECT author_id FROM comments WHERE id = $parentCommentID"""
            .queryWithLabel[UUID](show"Get comment author for Notification, Comment=$parentCommentID")
            .option
            .transact(transactor)
            .flatMap {
              case Some(parentAuthorUUID) if parentAuthorUUID =!= event.authorID.unwrap =>
                for {
                  notifID <- ID.create[F, io.branchtalk.notifications.model.Notification]
                  now <- CreationTime.now[F]
                  correlationID = event.correlationID
                } yield Some(
                  notifID.unwrap -> NotificationCommandEvent.Create(
                    id = notifID,
                    recipientID = ID[User](parentAuthorUUID),
                    kind = Notification.Kind.CommentReply,
                    sourcePostID = Some(ID[Post](event.postID.unwrap)),
                    sourceCommentID = Some(ID[Comment](event.id.unwrap)),
                    sourceUserID = Some(commentAuthorID),
                    message = Notification.Message.unsafeMake("Someone replied to your comment"),
                    createdAt = now,
                    correlationID = correlationID
                  )
                )
              case _ => none[(UUID, NotificationCommandEvent)].pure[F]
            }
        case None => none[(UUID, NotificationCommandEvent)].pure[F]
      }

    (postReplyNotification, commentReplyNotification).mapN { (postNotif, commentNotif) =>
      List(postNotif, commentNotif).flatten
    }
  }
}
