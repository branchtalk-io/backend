package io.branchtalk.shared.model

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative

final case class Paginated[+Entity](entities: List[Entity], nextOffset: Option[Long Refined NonNegative]) {

  def map[B](f: Entity => B): Paginated[B] = Paginated(entities.map(f), nextOffset)
}
object Paginated {

  def empty[Entity]: Paginated[Entity] = Paginated(entities = List.empty, nextOffset = None)
}
