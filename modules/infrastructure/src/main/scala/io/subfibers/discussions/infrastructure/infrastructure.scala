package io.subfibers.discussions

import fs2.concurrent.Topic
import io.subfibers.discussions.events.{ CommentEvent, PostEvent }

package object infrastructure {

  type CommentEventBus[F[_]] = Topic[F, CommentEvent]
  type PostEventBus[F[_]]    = Topic[F, PostEvent]
}
