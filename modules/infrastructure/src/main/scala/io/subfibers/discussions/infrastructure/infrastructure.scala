package io.subfibers.discussions

import fs2.concurrent.Topic
import io.subfibers.discussions.events.{ CommentEvent, PostEvent }

package object infrastructure {

  // TODO: it doesn't make sens, so lets replace content od this directory with some projections

  type CommentEventBus[F[_]] = Topic[F, CommentEvent]
  type PostEventBus[F[_]]    = Topic[F, PostEvent]
}
