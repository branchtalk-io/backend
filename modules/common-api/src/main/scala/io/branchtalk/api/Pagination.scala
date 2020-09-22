package io.branchtalk.api

import io.scalaland.chimney.dsl._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.shared.models.Paginated

final case class Pagination[A](
  entities:   List[A],
  offset:     PaginationOffset,
  limit:      PaginationLimit,
  nextOffset: Option[PaginationOffset]
)
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

  @SuppressWarnings(
    // for macros
    Array(
      "org.wartremover.warts.Equals",
      "org.wartremover.warts.Null",
      "org.wartremover.warts.OptionPartial",
      "org.wartremover.warts.TraversableOps",
      "org.wartremover.warts.Var",
      "org.wartremover.warts.While"
    )
  )
  implicit def jsoniterValueCodec[A: JsonValueCodec]: JsonValueCodec[Pagination[A]] =
    JsonCodecMaker.make[Pagination[A]]

  // TODO
//  implicit def schema[A: Schema]: Schema[Pagination[A]] =
}
