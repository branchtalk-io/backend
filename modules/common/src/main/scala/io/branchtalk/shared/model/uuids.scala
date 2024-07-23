package io.branchtalk.shared.model

import cats.effect.Sync
import com.eatthepath.uuid.FastUUID
import com.fasterxml.uuid.Generators

type UUID = java.util.UUID
object UUID {

  def create[F[_]: Sync](implicit uuidGenerator: UUIDGenerator): F[UUID] = uuidGenerator.create[F]
  def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[UUID] = uuidGenerator.parse[F](string)
}

trait UUIDGenerator {

  def create[F[_]: Sync]:                 F[UUID]
  def parse[F[_]:  Sync](string: String): F[UUID]
}

object UUIDGenerator {
  object FastUUIDGenerator extends UUIDGenerator {
    override def create[F[_]: Sync]:                 F[UUID] = Sync[F].delay(Generators.timeBasedGenerator().generate())
    override def parse[F[_]:  Sync](string: String): F[UUID] = Sync[F].delay(FastUUID.parseUUID(string))
  }
}
