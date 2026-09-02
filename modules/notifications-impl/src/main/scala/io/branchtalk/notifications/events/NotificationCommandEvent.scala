package io.branchtalk.notifications.events

import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.logging.*
import io.branchtalk.notifications.model.{ Comment, Notification, Post, User }
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

sealed trait NotificationCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
object NotificationCommandEvent {

  final case class Create(
    id:              ID[Notification],
    recipientID:     ID[User],
    kind:            Notification.Kind,
    sourcePostID:    Option[ID[Post]],
    sourceCommentID: Option[ID[Comment]],
    sourceUserID:    Option[ID[User]],
    message:         Notification.Message,
    createdAt:       CreationTime,
    correlationID:   CorrelationID
  ) extends NotificationCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class MarkRead(
    id:            ID[Notification],
    userID:        ID[User],
    readAt:        ModificationTime,
    correlationID: CorrelationID
  ) extends NotificationCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty

  final case class MarkAllRead(
    recipientID:   ID[User],
    readAt:        ModificationTime,
    correlationID: CorrelationID
  ) extends NotificationCommandEvent derives AvroEncoder, AvroDecoder, FastEq, ShowPretty
}
