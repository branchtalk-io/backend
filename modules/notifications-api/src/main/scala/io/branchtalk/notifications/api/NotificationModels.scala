package io.branchtalk.notifications.api

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.notifications.model.{ Comment, Notification, Post, User }
import io.branchtalk.shared.model.{ CreationTime, ID, ModificationTime, branchtalkLocale }

object NotificationModels {

  // Notification.Kind is a Scala 3 enum -- provide JSON codec and Tapir schema manually.
  @SuppressWarnings(Array("org.wartremover.warts.ToString", "org.wartremover.warts.Equals"))
  given JsCodec[Notification.Kind] = DefaultJsCodec.derived[String].mapDecode[Notification.Kind] { s =>
    try Right(Notification.Kind.fromString(s))
    catch { case _: NoSuchElementException => Left(s"Unknown notification kind: $s") }
  }(_.toString.toLowerCase(branchtalkLocale))

  @SuppressWarnings(Array("org.wartremover.warts.ToString", "org.wartremover.warts.Equals"))
  given JsSchema[Notification.Kind] = JsSchema.schemaForString.map[Notification.Kind] { s =>
    Notification.Kind.values.find(_.toString.toLowerCase(branchtalkLocale) == s.toLowerCase(branchtalkLocale))
  }(_.toString.toLowerCase(branchtalkLocale))

  sealed trait NotificationError derives DefaultJsCodec, JsSchema
  object NotificationError {

    final case class BadCredentials(msg: String) extends NotificationError derives DefaultJsCodec, JsSchema
    final case class NoPermission(msg: String) extends NotificationError derives DefaultJsCodec, JsSchema
    final case class NotFound(msg: String) extends NotificationError derives DefaultJsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends NotificationError
        derives DefaultJsCodec,
          JsSchema
  }

  final case class APINotification(
    id:              ID[Notification],
    recipientID:     ID[User],
    kind:            Notification.Kind,
    sourcePostID:    Option[ID[Post]],
    sourceCommentID: Option[ID[Comment]],
    sourceUserID:    Option[ID[User]],
    message:         Notification.Message,
    createdAt:       CreationTime,
    readAt:          Option[ModificationTime]
  ) derives DefaultJsCodec,
        JsSchema
  object APINotification {

    def fromDomain(notification: Notification): APINotification =
      APINotification(
        id = notification.id,
        recipientID = notification.data.recipientID,
        kind = notification.data.kind,
        sourcePostID = notification.data.sourcePostID,
        sourceCommentID = notification.data.sourceCommentID,
        sourceUserID = notification.data.sourceUserID,
        message = notification.data.message,
        createdAt = notification.data.createdAt,
        readAt = notification.data.readAt
      )
  }

  final case class MarkReadResponse(id: ID[Notification]) derives DefaultJsCodec, JsSchema

  final case class MarkAllReadResponse(count: Long) derives DefaultJsCodec, JsSchema
}
