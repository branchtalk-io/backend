package io.branchtalk.users.model

import java.security.SecureRandom

import cats.{ Eq, Order, Show }
import cats.effect.Sync
import enumeratum._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import io.branchtalk.shared.models.{ FastEq, ParseRefined, ShowPretty }
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi

trait UserProperties {
  type Email       = UserProperties.Email
  type Description = UserProperties.Description
  type Password    = UserProperties.Password
  val Email       = UserProperties.Email
  val Description = UserProperties.Description
  val Password    = UserProperties.Password
}
object UserProperties {

  @newtype final case class Email(value: String Refined MatchesRegex["(.+)@(.+)"])
  object Email {
    def parse[F[_]: Sync](string: String): F[Email] =
      ParseRefined[F].parse[MatchesRegex["(.+)@(.+)"]](string).map(Email.apply)

    implicit val show:  Show[Email]  = (t: Email) => s"Email(${t.value.value.show})"
    implicit val order: Order[Email] = (x: Email, y: Email) => x.value.value compareTo y.value.value
  }

  @newtype final case class Description(value: String)
  object Description {

    implicit val show: Show[Description] = (t: Description) => s"Email(${t.value.show})"
    implicit val eq:   Eq[Description]   = (x: Description, y: Description) => x.value === y.value
  }

  @Semi(FastEq, ShowPretty) final case class Password(
    algorithm: Password.Algorithm,
    hash:      Password.Hash,
    salt:      Password.Salt
  ) {

    def update(raw: Password.Raw): Password = copy(hash = algorithm.hashRaw(raw, salt))
    def verify(raw: Password.Raw): Boolean  = algorithm.verify(raw, salt, hash)
  }
  object Password {

    @Semi(FastEq, ShowPretty) sealed trait Algorithm extends EnumEntry {

      def createSalt: Password.Salt
      def hashRaw(raw: Password.Raw, salt: Password.Salt): Password.Hash
      def verify(raw:  Password.Raw, salt: Password.Salt, hash: Password.Hash): Boolean
    }
    object Algorithm extends Enum[Algorithm] {
      private lazy val sr = new SecureRandom()

      case object BCrypt extends Algorithm {
        private val cost = 1000 // TODO: make it configurable or sth?

        private val hasher   = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
        private val verifier = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()

        override def createSalt: Password.Salt = {
          val bytes = new Array[Byte](16) // required by BCrypt to have 16 bytes
          sr.nextBytes(bytes)
          Password.Salt(bytes)
        }

        override def hashRaw(raw: Password.Raw, salt: Password.Salt): Password.Hash =
          Password.Hash(hasher.hashRaw(cost, salt.bytes, raw.bytes).rawHash)

        override def verify(raw: Password.Raw, salt: Password.Salt, hash: Password.Hash): Boolean =
          verifier.verify(raw.bytes, cost, salt.bytes, hash.bytes).verified
      }

      def default: Algorithm = BCrypt // TODO: use config to change this when more than one option is available

      val values = findValues
    }

    @newtype final case class Hash(bytes: Array[Byte])
    object Hash {

      implicit val show: Show[Hash] = (_: Hash) => s"Password.Hash(EDITED OUT)"
      implicit val eq:   Eq[Hash]   = (x: Hash, y: Hash) => x.bytes sameElements y.bytes
    }

    @newtype final case class Salt(bytes: Array[Byte])
    object Salt {

      implicit val show: Show[Salt] = (_: Salt) => s"Password.Salt(EDITED OUT)"
      implicit val eq:   Eq[Salt]   = (x: Salt, y: Salt) => x.bytes sameElements y.bytes
    }

    @newtype final case class Raw(bytes: Array[Byte])
    object Raw {

      implicit val show: Show[Raw] = (_: Raw) => s"Password.Raw(EDITED OUT)"
      implicit val eq:   Eq[Raw]   = (x: Raw, y: Raw) => x.bytes sameElements y.bytes
    }

    def create(raw: Password.Raw): Password = {
      val algorithm = Password.Algorithm.default
      val salt      = algorithm.createSalt
      val hash      = algorithm.hashRaw(raw, salt)
      Password(algorithm, hash, salt)
    }
  }

  // TODO: permissions

  // TODO: settings
}
