package io.branchtalk.discussions

import cats.data.NonEmptyList
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Sync, Timer }
import io.branchtalk.discussions.events.{ DiscussionCommandEvent, DiscussionEvent }
import io.branchtalk.discussions.reads._
import io.branchtalk.discussions.writes._
import io.branchtalk.shared.models._
import io.branchtalk.shared.infrastructure._
import fs2._
import fs2.kafka._
import _root_.io.branchtalk.discussions.reads.ChannelReads
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._

final case class DiscussionsReads[F[_]](
  channelReads: ChannelReads[F],
  postReads:    PostReads[F],
  commentReads: CommentReads[F]
)

final case class DiscussionsWrites[F[_]](
  commentWrites: CommentWrites[F],
  postWrites:    PostWrites[F],
  channelWrites: ChannelWrites[F],
  runProjector:  Resource[F, F[Unit]]
)

object DiscussionsModule extends DomainModule[DiscussionEvent, DiscussionCommandEvent] {

  private val logger = Logger(getClass)

  def reads[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig: DomainConfig
  ): Resource[F, DiscussionsReads[F]] =
    setupReads[F](domainConfig).map {
      case ReadsInfrastructure(transactor, _) =>
        val channelReads: ChannelReads[F] = new ChannelReadsImpl[F](transactor)
        val postReads:    PostReads[F]    = new PostReadsImpl[F](transactor)
        val commentReads: CommentReads[F] = new CommentReadsImpl[F](transactor)

        DiscussionsReads(channelReads, postReads, commentReads)
    }

  def writes[F[_]: ConcurrentEffect: ContextShift: Timer](
    domainConfig:           DomainConfig
  )(implicit uuidGenerator: UUIDGenerator): Resource[F, DiscussionsWrites[F]] =
    setupWrites[F](domainConfig).map {
      case WritesInfrastructure(transactor, internalProducer, internalConsumerStream, producer) =>
        val commentRepository: CommentWrites[F] = new CommentWritesImpl[F](internalProducer)
        val postRepository:    PostWrites[F]    = new PostWritesImpl[F](internalProducer)
        val channelRepository: ChannelWrites[F] = new ChannelWritesImpl[F](internalProducer)

        val projector: Projector[F, DiscussionCommandEvent, (UUID, DiscussionEvent)] = NonEmptyList
          .of(
            new ChannelProjector[F](transactor),
            new CommentProjector[F](transactor),
            new PostProjector[F](transactor)
          )
          .reduce
        val runProjector = internalConsumerStream.withPipeToResource(logger)(projector andThen producer)

        DiscussionsWrites(commentRepository, postRepository, channelRepository, runProjector)
    }
}
