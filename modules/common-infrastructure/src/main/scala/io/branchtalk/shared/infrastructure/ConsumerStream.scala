package io.branchtalk.shared.infrastructure

import cats.effect.{ Resource, Sync }
import com.typesafe.scalalogging.Logger
import fs2._

final case class ConsumerStream[F[_], Event](
  consumer:  EventBusConsumer[F, Event],
  committer: EventBusCommitter[F]
) {

  def withPipeToResource[B](logger: Logger)(f: Pipe[F, Event, B])(implicit F: Sync[F]): Resource[F, F[Unit]] =
    KillSwitch.asStream[F, F[Unit]] { stream =>
      consumer
        .zip(stream)
        .flatMap {
          case (event, _) =>
            Stream(event.record.value)
              .evalTap(_ => F.delay(logger.info(s"Processing event key = ${event.record.key}")))
              .through(f)
              .map(_ => event.offset)
        }
        .through(committer)
        .compile
        .drain
    }
}
