package io.branchtalk.users.model

import cats.{ Order, Show }
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import io.branchtalk.shared.models.ParseRefined
import io.estatico.newtype.macros.newtype

trait UserProperties {
  type Email = UserProperties.Email
  val Email = UserProperties.Email
}
object UserProperties {

  @newtype final case class Email(value: String Refined MatchesRegex["(.+)@(.+)"])
  object Email {
    def parse[F[_]: Sync](string: String): F[Email] =
      ParseRefined[F].parse[MatchesRegex["(.+)@(.+)"]](string).map(Email.apply)

    implicit val show:  Show[Email]  = (t: Email) => s"Email(${t.value.value.show})"
    implicit val order: Order[Email] = (x: Email, y: Email) => x.value.value compareTo y.value.value
  }

  // profile description

  // hashed password

  // password salt

  // permissions

  // settings
}
