package io.branchtalk.shared.infrastructure

import cats.Show
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.branchtalk.shared.model._

// not @newtype to allow overriding toString
final case class DatabasePassword(nonEmptyString: NonEmptyString) {
  override def toString: String = "[PASSWORD]"
}
object DatabasePassword {
  def unapply(databasePassword: DatabasePassword): Some[NonEmptyString] = Some(databasePassword.nonEmptyString)

  implicit val configReader: ConfigReader[DatabasePassword] = ConfigReader[NonEmptyString].map(DatabasePassword(_))
  implicit val show:         Show[DatabasePassword]         = Show.wrap(_.toString)
}
