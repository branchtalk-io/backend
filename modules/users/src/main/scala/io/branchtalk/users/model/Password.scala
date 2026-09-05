package io.branchtalk.users.model

import java.security.SecureRandom

import cats.{ Eq, Show }
import cats.effect.{ Sync, SyncIO }
import enumeratum.{ Enum, EnumEntry }
import enumeratum.EnumEntry.Hyphencase
import io.branchtalk.shared.model.*

final case class Password(
  algorithm: Password.Algorithm,
  hash:      Password.Hash,
  salt:      Password.Salt
) derives FastEq,
      ShowPretty {

  def update(raw: Password.Raw): Password = copy(hash = algorithm.hashRaw(raw, salt))
  def verify(raw: Password.Raw): Boolean  = algorithm.verify(raw, salt, hash)

  // allows comparison of Passwords which would otherwise use Array's hashCode method

  override def equals(other: Any): Boolean = other match {
    case Password(`algorithm`, otherHash, otherSalt) => hash === otherHash && salt === otherSalt
    case _                                           => false
  }

  override def hashCode(): Int = algorithm.hashCode() ^ hash.unwrap.toSeq.hashCode() ^ salt.unwrap.toSeq.hashCode()
}
object Password {

  sealed trait Algorithm extends EnumEntry with Hyphencase derives FastEq, ShowPretty {

    def createSalt:                                                           Password.Salt
    def hashRaw(raw: Password.Raw, salt: Password.Salt):                      Password.Hash
    def verify(raw:  Password.Raw, salt: Password.Salt, hash: Password.Hash): Boolean
  }
  object Algorithm extends Enum[Algorithm] {
    private lazy val sr = new SecureRandom()

    case object BCrypt extends Algorithm {
      // Default cost; callers can override via hashRawWithCost / verifyWithCost.
      private val defaultCost = 10 // must be between 4 and 31

      private val hasher   = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
      private val verifier = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()

      override def entryName: String = "bcrypt"

      override def createSalt: Password.Salt = {
        val bytes = new Array[Byte](at.favre.lib.crypto.bcrypt.BCrypt.SALT_LENGTH)
        sr.nextBytes(bytes)
        Password.Salt(bytes)
      }

      override def hashRaw(raw: Password.Raw, salt: Password.Salt): Password.Hash =
        hashRawWithCost(raw, salt, defaultCost)

      def hashRawWithCost(raw: Password.Raw, salt: Password.Salt, cost: Int): Password.Hash =
        Password.Hash(hasher.hashRaw(cost, salt.unwrap, raw.unwrap).rawHash)

      override def verify(raw: Password.Raw, salt: Password.Salt, hash: Password.Hash): Boolean =
        verifyWithCost(raw, salt, hash, defaultCost)

      def verifyWithCost(raw: Password.Raw, salt: Password.Salt, hash: Password.Hash, cost: Int): Boolean =
        verifier.verify(raw.unwrap, cost, salt.unwrap, hash.unwrap).verified
    }

    def default: Algorithm = BCrypt

    val values: IndexedSeq[Algorithm] = findValues
  }

  private val arrayEq: Eq[Array[Byte]] = _ sameElements _

  type Hash = Hash.Type
  object Hash extends Newtype[Array[Byte]] {
    def unapply(hash: Hash): Some[Array[Byte]] = Some(hash.unwrap)

    given Show[Hash] = _ => "EDITED OUT"
    given Eq[Hash]   = unsafeMakeF[Eq](arrayEq)
  }

  type Salt = Salt.Type
  object Salt extends Newtype[Array[Byte]] {
    def unapply(salt: Salt): Some[Array[Byte]] = Some(salt.unwrap)

    given Show[Salt] = _ => "EDITED OUT"
    given Eq[Salt]   = unsafeMakeF[Eq](arrayEq)
  }

  type Raw = Raw.Type
  object Raw extends Newtype[Array[Byte]] {
    override inline def validate(input: Array[Byte]): Boolean = input.nonEmpty

    def unapply(hash: Raw): Some[Array[Byte]] = Some(hash.unwrap)

    def fromString(string: String): Either[String, Raw] = make(string.getBytes(branchtalkCharset))

    given Show[Raw] = _ => "EDITED OUT"
    given Eq[Raw]   = unsafeMakeF[Eq](arrayEq)
  }

  final case class Config(
    algorithm:  String = "bcrypt",
    bcryptCost: Int = 10
  )

  def create(raw: Password.Raw, config: Config = Config()): Password = {
    val algorithm = Password.Algorithm.default
    val salt      = algorithm.createSalt
    val hash = algorithm match {
      case Password.Algorithm.BCrypt => Password.Algorithm.BCrypt.hashRawWithCost(raw, salt, config.bcryptCost)
    }
    Password(algorithm, hash, salt)
  }
}
