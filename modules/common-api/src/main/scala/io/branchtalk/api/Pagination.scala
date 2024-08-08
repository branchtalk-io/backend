package io.branchtalk.api

import cats.effect.Sync
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.api.TapirSupport.*
import io.scalaland.chimney.dsl.*
import io.branchtalk.shared.model.*

final case class Pagination[A](
  entities:   List[A],
  offset:     Pagination.Offset,
  limit:      Pagination.Limit,
  nextOffset: Option[Pagination.Offset]
) derives DefaultJsCodec,
      JsSchema

object Pagination {

  def fromPaginated[Entity](
    paginated: Paginated[Entity],
    offset:    Offset,
    limit:     Limit
  ): Pagination[Entity] =
    paginated
      .into[Pagination[Entity]]
      .withFieldConst(_.offset, offset)
      .withFieldConst(_.limit, limit)
      .withFieldComputed(_.nextOffset, _.nextOffset.map(o => Offset.unsafeMake(o.unwrap)))
      .transform

  type Offset = Offset.Type
  object Offset extends Newtype[Long] {

    override inline def validate(input: Long): Boolean = input >= 0L

    def unapply(offset: Offset): Some[Long] = Some(offset.unwrap)
    def parse[F[_]: Sync](long: Long): F[Offset] = ParseNewtype[F].parse[Offset](long)

    given JsCodec[Offset] = DefaultJsCodec.derived[Long].asNewtypeCodec[Offset]
    given Param[Offset] = summonParam[Long].mapDecode(l => DecodeResult.fromEitherString(l.toString, make(l)))(_.unwrap)
    given JsSchema[Offset] = summonSchema[Long].asNewtypeSchema[Offset]
  }

  type Limit = Limit.Type
  object Limit extends Newtype[Long] {

    override inline def validate(input: Long): Boolean = input > 0L

    def unapply(limit: Limit): Some[Long] = Some(limit.unwrap)
    def parse[F[_]: Sync](long: Long): F[Limit] = ParseNewtype[F].parse[Limit](long)

    given JsCodec[Limit] = DefaultJsCodec.derived[Long].asNewtypeCodec[Limit]
    given Param[Limit] = summonParam[Long].mapDecode(l => DecodeResult.fromEitherString(l.toString, make(l)))(_.unwrap)
    given JsSchema[Limit] = summonSchema[Long].asNewtypeSchema[Limit]
  }

  type HasNext = HasNext.Type
  object HasNext extends Newtype[Boolean] {

    def unapply(hasNext: HasNext): Some[Boolean] = Some(hasNext.unwrap)

    given JsCodec[HasNext]  = DefaultJsCodec.derived[Boolean].asNewtypeCodec[HasNext]
    given JsSchema[HasNext] = summonSchema[Boolean].asNewtypeSchema[HasNext]
  }
}
