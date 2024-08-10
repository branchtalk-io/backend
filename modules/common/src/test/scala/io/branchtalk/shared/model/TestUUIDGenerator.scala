package io.branchtalk.shared.model

import cats.effect.Sync

import scala.collection.mutable

class TestUUIDGenerator extends UUID.Generator {

  private val queue = mutable.Queue.empty[UUID]

  def stubNext(uuid: UUID): Unit = synchronized {
    queue.enqueue(uuid)
    ()
  }

  def clean(): Unit = synchronized {
    queue.dequeueAll(_ => true)
    ()
  }

  override def create[F[_]: Sync]: F[UUID] = synchronized {
    if (queue.isEmpty) UUID.FastGenerator.create[F]
    else queue.dequeue().pure[F]
  }

  override def parse[F[_]: Sync](string: String): F[UUID] = UUID.FastGenerator.parse[F](string)
}
