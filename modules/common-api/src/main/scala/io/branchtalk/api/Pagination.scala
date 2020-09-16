package io.branchtalk.api

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._

final case class Pagination[A](
  entries: List[A],
  offset:  PaginationOffset,
  limit:   PaginationLimit,
  hasNext: PaginationHasNext
)
@SuppressWarnings(
  // for macros
  Array(
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.While"
  )
)
object Pagination {

  implicit def paginationJsoniterValueCodec[A: JsonValueCodec]: JsonValueCodec[Pagination[A]] =
    JsonCodecMaker.make[Pagination[A]]
}
