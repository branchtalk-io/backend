package io.branchtalk.notifications.infrastructure

import io.branchtalk.notifications.model.Notification
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.branchtalkLocale

object DoobieExtensions {

  @SuppressWarnings(
    Array("org.wartremover.warts.Throw", "org.wartremover.warts.ToString", "org.wartremover.warts.Equals")
  )
  given notificationKindMeta: Meta[Notification.Kind] = pgEnumString(
    "notification_kind",
    name =>
      Notification.Kind.values
        .find(_.toString.toLowerCase(branchtalkLocale) == name.toLowerCase(branchtalkLocale))
        .getOrElse(throw new NoSuchElementException(show"$name is not a member of Notification.Kind")),
    _.toString.toLowerCase(branchtalkLocale)
  )
}
