package io.branchtalk.discussions

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import cats.effect.concurrent.Ref
import cats.implicits._
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, DiscussionEvent }
import io.branchtalk.discussions.writes._
import io.branchtalk.shared.infrastructure._
import fs2._

final case class DiscussionsWrites[F[_]](
  commentRepository: CommentWrites[F],
  postRepository:    PostWrites[F],
  channelRepository: ChannelWrites[F],
  runProjector:      F[(F[Unit], F[Unit])]
)

object DiscussionsModule extends DomainModule[DiscussionEvent, DiscussionCommandEvent] {

  def writes[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, DiscussionsWrites[F]] =
    setupWrites[F](domainConfig).map {
      case WritesInfrastructure(transactor, internalPublisher, internalConsumer, publisher) =>
        val commentRepository: CommentWrites[F] = new CommentWritesImpl[F](internalPublisher)
        val postRepository:    PostWrites[F]    = new PostWritesImpl[F](internalPublisher)
        val channelRepository: ChannelWrites[F] = new ChannelWritesImpl[F](internalPublisher)

        val projector = NonEmptyList
          .of(
            new ChannelProjector[F](transactor),
            new CommentProjector[F](transactor),
            new PostProjector[F](transactor)
          )
          .reduce

        val runProjector = KillSwitch[F].map {
          case KillSwitch(stream, switch) =>
            internalConsumer
              .zip(stream)
              .flatMap {
                case (event, _) =>
                  Stream(event.record.value).through(projector).through(publisher).map(_ => event)
              }
              .evalMap(event => event.offset.commit)
              .compile
              .drain -> switch
        }

        DiscussionsWrites(commentRepository, postRepository, channelRepository, runProjector)
    }
}
