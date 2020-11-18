package io.branchtalk.users.model

import java.security.SecureRandom

import cats.{ Eq, Show }
import cats.effect.{ IO, Sync }
import enumeratum.{ Enum, EnumEntry }
import enumeratum.EnumEntry.Hyphencase
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.branchtalk.shared.model.{ FastEq, ParseRefined, ShowPretty }
import io.estatico.newtype.macros.newtype
import io.scalaland.catnip.Semi

@Semi(FastEq, ShowPretty) final case class Password(
  algorithm: Password.Algorithm,
  hash:      Password.Hash,
  salt:      Password.Salt
) {

  def update(raw: Password.Raw): Password = copy(hash = algorithm.hashRaw(raw, salt))
  def verify(raw: Password.Raw): Boolean  = algorithm.verify(raw, salt, hash)

  // allows comparison of Passwords which would otherwise use Array's hashCode method

  override def equals(other: Any): Boolean = other match {
    case Password(`algorithm`, otherHash, otherSalt)
        if hash.bytes.sameElements(otherHash.bytes) && salt.bytes.sameElements(otherSalt.bytes) =>
      true
    case _ => false
  }

  override def hashCode(): Int = algorithm.hashCode() ^ hash.bytes.toSeq.hashCode() ^ salt.bytes.toSeq.hashCode()
}
object Password {

  @Semi(FastEq, ShowPretty) sealed trait Algorithm extends EnumEntry with Hyphencase {

    def createSalt: Password.Salt
    def hashRaw(raw: Password.Raw, salt: Password.Salt): Password.Hash
    def verify(raw:  Password.Raw, salt: Password.Salt, hash: Password.Hash): Boolean
  }
  object Algorithm extends Enum[Algorithm] {
    private lazy val sr = new SecureRandom()

    case object BCrypt extends Algorithm {
      private val cost = 10 // must be between 4 and 31

      private val hasher   = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
      private val verifier = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()

      override def entryName: String = "bcrypt"

      override def createSalt: Password.Salt = {
        val bytes = new Array[Byte](16) // required by BCrypt to have 16 bytes
        sr.nextBytes(bytes)
        Password.Salt(bytes)
      }

      override def hashRaw(raw: Password.Raw, salt: Password.Salt): Password.Hash =
        Password.Hash(hasher.hashRaw(cost, salt.bytes, raw.nonEmptyBytes).rawHash)

      override def verify(raw: Password.Raw, salt: Password.Salt, hash: Password.Hash): Boolean =
        verifier.verify(raw.nonEmptyBytes, cost, salt.bytes, hash.bytes).verified
    }

    def default: Algorithm = BCrypt

    val values: IndexedSeq[Algorithm] = findValues
  }

  @newtype final case class Hash(bytes: Array[Byte])
  object Hash {
    def unapply(hash: Hash): Option[Array[Byte]] = hash.bytes.some

    implicit val show: Show[Hash] = (_: Hash) => s"Password.Hash(EDITED OUT)"
    implicit val eq:   Eq[Hash]   = (x: Hash, y: Hash) => x.bytes sameElements y.bytes
  }

  @newtype final case class Salt(bytes: Array[Byte])
  object Salt {
    def unapply(salt: Salt): Option[Array[Byte]] = salt.bytes.some

    implicit val show: Show[Salt] = (_: Salt) => s"Password.Salt(EDITED OUT)"
    implicit val eq:   Eq[Salt]   = (x: Salt, y: Salt) => x.bytes sameElements y.bytes
  }

  @newtype final case class Raw(nonEmptyBytes: Array[Byte] Refined NonEmpty)
  object Raw {
    def unapply(raw: Raw): Option[Array[Byte] Refined NonEmpty] = raw.nonEmptyBytes.some
    def parse[F[_]: Sync](bytes: Array[Byte]): F[Raw] =
      ParseRefined[F].parse[NonEmpty](bytes).map(Raw.apply)

    def fromString(string: String Refined NonEmpty): Raw =
      Raw(ParseRefined[IO].parse[NonEmpty](string.getBytes).unsafeRunSync())

    implicit val show: Show[Raw] = (_: Raw) => s"Password.Raw(EDITED OUT)"
    implicit val eq:   Eq[Raw]   = (x: Raw, y: Raw) => x.nonEmptyBytes.value sameElements y.nonEmptyBytes.value
  }

  def create(raw: Password.Raw): Password = {
    val algorithm = Password.Algorithm.default
    val salt      = algorithm.createSalt
    val hash      = algorithm.hashRaw(raw, salt)
    Password(algorithm, hash, salt)
  }
}
