package io.branchtalk.shared.model

import cats.effect.Sync
import com.eatthepath.uuid.FastUUID
import com.fasterxml.uuid.Generators
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Uuid

trait UUIDGenerator {

  def apply(string: String Refined Uuid): UUID
  def create[F[_]: Sync]: F[UUID]
  def parse[F[_]:  Sync](string: String): F[UUID]
}

object UUIDGenerator {
  object FastUUIDGenerator extends UUIDGenerator {
    override def apply(string: Refined[String, Uuid]): UUID = FastUUID.parseUUID(string.value)
    override def create[F[_]: Sync]: F[UUID] = Sync[F].delay(Generators.timeBasedGenerator().generate())
    override def parse[F[_]:  Sync](string: String): F[UUID] = Sync[F].delay(FastUUID.parseUUID(string))
  }
}
