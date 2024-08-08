package io.branchtalk.shared.model

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
  }

  type Limit = Limit.Type
  object Limit extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input > 0
  }
}
