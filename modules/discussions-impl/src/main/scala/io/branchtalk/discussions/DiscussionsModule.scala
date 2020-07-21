package io.branchtalk.discussions

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, DiscussionEvent }
import io.branchtalk.discussions.reads._
import io.branchtalk.discussions.writes._
import io.branchtalk.shared.infrastructure._
import fs2._
import _root_.io.branchtalk.discussions.reads.ChannelReads

final case class DiscussionsReads[F[_]](
  channelReads: ChannelReads[F]
)

final case class DiscussionsWrites[F[_]](
  commentWrites: CommentWrites[F],
  postWrites:    PostWrites[F],
  channelWrites: ChannelWrites[F],
  runProjector:  Resource[F, F[Unit]]
)

object DiscussionsModule extends DomainModule[DiscussionEvent, DiscussionCommandEvent] {

  def reads[F[_]: ConcurrentEffect: ContextShift: Timer](domainConfig: DomainConfig): Resource[F, DiscussionsReads[F]] =
    setupReads[F](domainConfig).map {
      case ReadsInfrastructure(transactor, _) =>
        val channelReads: ChannelReads[F] = new ChannelReadsImpl[F](transactor)

        DiscussionsReads(channelReads)
    }

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

        val runProjector = projectorAsResource(
          KillSwitch[F].map {
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
        )

        DiscussionsWrites(commentRepository, postRepository, channelRepository, runProjector)
    }
}
