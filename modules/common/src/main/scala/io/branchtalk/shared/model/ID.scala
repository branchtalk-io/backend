package io.branchtalk.shared.model

import cats.{ Order, Show }
import cats.effect.Sync
import neotype.*

type ID[Entity] = ID.Type[Entity]
object ID extends NewtypeT[UUID] {

  def unapply[Entity](entity: ID[Entity]): Some[UUID] = Some(entity.unwrap)
  def create[F[_]: Sync, Entity](using UUID.Generator): F[ID[Entity]] = UUID.create[F].map(unsafeMake[Entity])
  def parse[F[_]: Sync, Entity](string: String)(using UUID.Generator): F[ID[Entity]] =
    UUID.parse[F](string).map(unsafeMake[Entity])

  given [Entity]: Show[ID[Entity]]  = unsafeMakeF[Show, Entity](Show[UUID])
  given [Entity]: Order[ID[Entity]] = unsafeMakeF[Order, Entity](Order[UUID])
}
