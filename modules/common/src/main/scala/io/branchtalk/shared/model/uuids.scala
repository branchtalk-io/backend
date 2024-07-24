package io.branchtalk.shared.model

import cats.effect.Sync
import com.eatthepath.uuid.FastUUID
import com.fasterxml.uuid.Generators

type UUID = java.util.UUID
object UUID {

  val empty: UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

  def create[F[_]: Sync](using generator: Generator):                          F[UUID] = generator.create[F]
  def parse[F[_]:  Sync](string:          String)(using generator: Generator): F[UUID] = generator.parse[F](string)

  trait Generator {
    def create[F[_]: Sync]:                 F[UUID]
    def parse[F[_]:  Sync](string: String): F[UUID]
  }
  object FastGenerator extends Generator {
    override def create[F[_]: Sync]:                 F[UUID] = Sync[F].delay(Generators.timeBasedGenerator().generate())
    override def parse[F[_]:  Sync](string: String): F[UUID] = Sync[F].delay(FastUUID.parseUUID(string))
  }
}
