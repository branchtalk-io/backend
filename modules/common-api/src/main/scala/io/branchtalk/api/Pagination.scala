package io.branchtalk.api

import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.api.TapirSupport.JsSchema
import io.scalaland.chimney.dsl._
import io.branchtalk.shared.model.Paginated
import io.scalaland.catnip.Semi

@Semi(JsCodec, JsSchema) final case class Pagination[A](
  entities:   List[A],
  offset:     PaginationOffset,
  limit:      PaginationLimit,
  nextOffset: Option[PaginationOffset]
)

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object Pagination {

  def fromPaginated[Entity](
    paginated: Paginated[Entity],
    offset:    PaginationOffset,
    limit:     PaginationLimit
  ): Pagination[Entity] =
    paginated
      .into[Pagination[Entity]]
      .withFieldConst(_.offset, offset)
      .withFieldConst(_.limit, limit)
      .withFieldComputed(_.nextOffset, p => p.nextOffset.map(PaginationOffset(_)))
      .transform
}
