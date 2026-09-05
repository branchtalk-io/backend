package io.branchtalk.notifications.writes

import cats.Functor
import cats.effect.{ Concurrent, Resource }
import fs2.Stream
import fs2.concurrent.Topic
import io.branchtalk.notifications.model.{ Notification, User }
import io.branchtalk.shared.model.ID

// An fs2 Topic that the notification projector publishes to.
// Websocket connections subscribe, filtered by recipientID.
final class NotificationTopic[F[_]: Functor](topic: Topic[F, Option[Notification]]) {

  def publish(notification: Notification): F[Unit] =
    Functor[F].void(topic.publish1(Some(notification)))

  def subscribe(recipientID: ID[User]): Stream[F, Notification] =
    topic.subscribe(128).collect { case Some(n) if n.data.recipientID === recipientID => n }
}
object NotificationTopic {

  def create[F[_]: Concurrent]: Resource[F, NotificationTopic[F]] =
    Resource.eval(Topic[F, Option[Notification]]).map(new NotificationTopic(_))
}
