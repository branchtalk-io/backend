package io.subfibers.discussions

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import io.subfibers.discussions.events.{ DiscussionCommandEvent, DiscussionEvent }
import io.subfibers.discussions.infrastructure._
import io.subfibers.shared.infrastructure._
import io.subfibers.shared.models._

final case class DiscussionsModule[F[_]](
  commentRepository: CommentRepository[F],
  postRepository:    PostRepository[F],
  channelRepository: ChannelRepository[F],
  eventConsumer:     EventBusConsumer[F, UUID, DiscussionEvent]
)
object DiscussionsModule extends DomainModule[DiscussionEvent, DiscussionCommandEvent] {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, DiscussionsModule[F]] =
    setupInfrastructure[F](domainConfig).map {
      case Infrastructure(transactor, internalPublisher, _, _, consumer) =>
        val commentRepository: CommentRepository[F] = new CommentRepositoryImpl[F](transactor, internalPublisher)
        val postRepository:    PostRepository[F]    = new PostRepositoryImpl[F](transactor, internalPublisher)
        val channelRepository: ChannelRepository[F] = new ChannelRepositoryImpl[F](transactor, internalPublisher)
        DiscussionsModule(commentRepository, postRepository, channelRepository, consumer)
    }
}
