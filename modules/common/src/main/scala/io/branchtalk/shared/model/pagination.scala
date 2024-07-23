package io.branchtalk.shared.model

import neotype.*

type Offset = Offset.Type
object Offset extends Newtype[Int] {

  override def validate(input: Int): Boolean | String = input >= 0
}

final case class Paginated[+Entity](entities: List[Entity], nextOffset: Option[Offset]) {

  def map[B](f: Entity => B): Paginated[B] = Paginated(entities.map(f), nextOffset)
}
object Paginated {

  def empty[Entity]: Paginated[Entity] = Paginated(entities = List.empty, nextOffset = None)
}
