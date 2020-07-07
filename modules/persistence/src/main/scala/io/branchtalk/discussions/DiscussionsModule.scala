package io.branchtalk.discussions

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, DiscussionEvent }
import io.branchtalk.discussions.writes._
import io.branchtalk.shared.infrastructure._
import io.branchtalk.shared.models._

// TODO: rethink approach when it comes to sharing streams
final case class DiscussionsModule[F[_]](
  commentRepository: CommentWrites[F],
  postRepository:    PostWrites[F],
  channelRepository: ChannelWrites[F],
  eventConsumer:     EventBusConsumer[F, UUID, DiscussionEvent]
)
object DiscussionsModule extends DomainModule[DiscussionEvent, DiscussionCommandEvent] {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, DiscussionsModule[F]] =
    setupInfrastructure[F](domainConfig).map {
      case Infrastructure(transactor, internalPublisher, _, _, consumer) =>
        val commentRepository: CommentWrites[F] = new CommentWritesImpl[F](transactor, internalPublisher)
        val postRepository:    PostWrites[F]    = new PostWritesImpl[F](transactor, internalPublisher)
        val channelRepository: ChannelWrites[F] = new ChannelWritesImpl[F](transactor, internalPublisher)
        // TODO: add projectors
        DiscussionsModule(commentRepository, postRepository, channelRepository, consumer)
    }
}
