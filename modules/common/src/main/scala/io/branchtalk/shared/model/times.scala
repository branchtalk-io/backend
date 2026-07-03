package io.branchtalk.shared.model

import java.time.{ OffsetDateTime, ZoneId }
import java.time.format.DateTimeFormatter
import cats.{ Functor, Order, Show }
import cats.effect.{ Clock, Sync }
import neotype.*

type CreationTime = CreationTime.Type
object CreationTime extends Newtype[OffsetDateTime] {
  def unapply(creationTime: CreationTime): Some[OffsetDateTime] = Some(creationTime.unwrap)
  def now[F[_]: Functor: Clock]: F[CreationTime] =
    Clock[F].realTimeInstant.map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault())).map(unsafeMake)

  given Show[CreationTime]      = unsafeMakeF[Show](_.pipe(DateTimeFormatter.ISO_INSTANT.format))
  given Order[CreationTime]     = unsafeMakeF[Order](Order.fromComparable)
  given AvroCodec[CreationTime] = AvroSupport.newtypeCodec
}

type ModificationTime = ModificationTime.Type
object ModificationTime extends Newtype[OffsetDateTime] {
  def unapply(ModificationTime: ModificationTime): Some[OffsetDateTime] = Some(ModificationTime.unwrap)
  def now[F[_]: Functor: Clock]: F[ModificationTime] =
    Clock[F].realTimeInstant.map(OffsetDateTime.ofInstant(_, ZoneId.systemDefault())).map(unsafeMake)

  given Show[ModificationTime]      = unsafeMakeF[Show](_.pipe(DateTimeFormatter.ISO_INSTANT.format))
  given Order[ModificationTime]     = unsafeMakeF[Order](Order.fromComparable)
  given AvroCodec[ModificationTime] = AvroSupport.newtypeCodec
}
