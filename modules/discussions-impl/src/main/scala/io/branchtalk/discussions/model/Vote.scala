package io.branchtalk.discussions.model

import enumeratum.{ Enum, EnumEntry }
import io.branchtalk.shared.model.ID

final case class Vote[Entity](
  id:       ID[Entity],
  voter_id: ID[User]
)
object Vote {

  sealed trait Type extends EnumEntry
  object Type extends Enum[Type] {
    case object Upvote extends Type
    case object Downvote extends Type

    def upvote:   Type = Upvote
    def downvote: Type = Downvote

    val values = findValues
  }
}
