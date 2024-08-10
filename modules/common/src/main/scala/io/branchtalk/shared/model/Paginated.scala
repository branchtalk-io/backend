package io.branchtalk.shared.model

import cats.{ Order, Show }
import neotype.*

final case class Paginated[+Entity](
  entities:   List[Entity],
  nextOffset: Option[Paginated.Offset]
) {

  def map[B](f: Entity => B): Paginated[B] = Paginated(entities.map(f), nextOffset)
}
object Paginated {

  def empty[Entity]: Paginated[Entity] = Paginated(entities = List.empty, nextOffset = None)

  type Offset = Offset.Type
  object Offset extends Newtype[Long] {

    override inline def validate(input: Long): Boolean = input >= 0L

    given Show[Offset]  = unsafeMakeF[Show](Show[Long])
    given Order[Offset] = unsafeMakeF[Order](Order[Long])
  }

  type Limit = Limit.Type
  object Limit extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input > 0

    given Show[Limit]  = unsafeMakeF[Show](Show[Int])
    given Order[Limit] = unsafeMakeF[Order](Order[Int])
  }
}
