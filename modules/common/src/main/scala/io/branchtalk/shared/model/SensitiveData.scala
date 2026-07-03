package io.branchtalk.shared.model

import cats.{ Eq, Show }
import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import enumeratum.*
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import neotype.*

import scala.collection.compat.immutable.ArraySeq
import scala.util.{ Random, Try }

// Express intent that some data should not be stored as unencrypted format.
final case class SensitiveData[A](value: A) derives FastEq, AvroEncoder, AvroDecoder {

  def encrypt(
    algorithm: SensitiveData.Algorithm,
    key:       SensitiveData.Key
  )(using AvroEncoder[A]): SensitiveData.Encrypted[A] = algorithm.encrypt[A](this, key)

  override def toString: String = "SENSITIVE DATA"
}
object SensitiveData {

  type Encrypted[A] = Encrypted.Type[A]
  object Encrypted extends NewtypeT[ArraySeq[Byte]] {

    extension [A](enc: Encrypted[A]) {
      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      )(using AvroDecoder[A]): DeserializationResult[SensitiveData[A]] =
        algorithm.decrypt[A](enc, key)
    }

    given [A]: AvroCodec[Encrypted[A]] with {
      private val E = summon[AvroEncoder[ArraySeq[Byte]]]
      private val D = summon[AvroDecoder[ArraySeq[Byte]]]
      def schema:                      org.apache.avro.Schema = E.schema
      def encode(value: Encrypted[A]): Any                    = E.encode(value.unwrap)
      def decode(value: Any):          Encrypted[A]           = Encrypted(D.decode(value))
    }

    given [A]: Show[Encrypted[A]] = _ => "ENCRYPTED"
    given [A]: Eq[Encrypted[A]]   = Eq.by(_.unwrap)
  }

  type Key = Key.Type
  object Key extends Newtype[ArraySeq[Byte]] {

    given Show[Key] = _ => "KEY"
    given Eq[Key]   = Eq.by(_.unwrap)
  }

  enum Algorithm extends EnumEntry, EnumEntry.Hyphencase {
    case Blowfish

    import Algorithm.*

    final def generateKey(): Key = this match {
      case Blowfish => Key(ArraySeq.from(Random.nextBytes(defaultKeySize)))
    }

    // Avro fails to correctly serialize-then-deserialize primitives so we have to use wrapper (record schema)

    final def encrypt[A: AvroEncoder](value: SensitiveData[A], key: Key): Encrypted[A] = this match {
      case Blowfish =>
        val cipher = blowfishCipher(key, Cipher.ENCRYPT_MODE)
        AvroSerialization.serializeUnsafe(value).pipe(cipher.doFinal).pipe(ArraySeq.from(_)).pipe(Encrypted(_))
    }

    final def decrypt[A: AvroDecoder](
      encrypted: Encrypted[A],
      key:       Key
    ): DeserializationResult[SensitiveData[A]] = this match {
      case Blowfish =>
        val cipher = blowfishCipher(key, Cipher.DECRYPT_MODE)
        Try(cipher.doFinal(encrypted.unwrap.toArray)).toEither.left
          .map(DeserializationError.DecodingError("SensitiveData decoding error", _))
          .flatMap(AvroSerialization.deserializeUnsafe[SensitiveData[A]](_))
    }
  }
  object Algorithm {

    private def getCipher(name: String)(key: Key, mode: Int) =
      Cipher.getInstance(name).tap(_.init(mode, new SecretKeySpec(key.unwrap.toArray, name)))

    private val defaultKeySize = 32
    private val blowfishCipher = getCipher("Blowfish") _

    def default: Algorithm = Blowfish // TODO: make configurable
  }

  given [A]: Show[A] = _ => "SENSITIVE DATA"
}
