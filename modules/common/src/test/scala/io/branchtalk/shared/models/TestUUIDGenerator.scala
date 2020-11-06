package io.branchtalk.shared.models

import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid

import scala.collection.mutable

class TestUUIDGenerator extends UUIDGenerator {

  private val queue = mutable.Queue.empty[UUID]

  def stubNext(uuid:   UUID):                Unit = synchronized(queue.enqueue(uuid))
  def stubNext(string: String Refined Uuid): Unit = synchronized(queue.enqueue(apply(string)))

  def clean(): Unit = synchronized {
    queue.dequeueAll(_ => true)
    ()
  }

  override def apply(string: String Refined Uuid): UUID = UUIDGenerator.FastUUIDGenerator(string)

  override def create[F[_]: Sync]: F[UUID] = synchronized {
    if (queue.isEmpty) UUIDGenerator.FastUUIDGenerator.create[F]
    else queue.dequeue().pure[F]
  }

  override def parse[F[_]: Sync](string: String): F[UUID] = UUIDGenerator.FastUUIDGenerator.parse[F](string)
}
