package io.branchtalk.shared.model

import cats.{ Eq, Show }
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import enumeratum.*
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import neotype.*

import scala.collection.compat.immutable.ArraySeq
import scala.util.{ Random, Try }

// Express intent that some data should not be stored as unencrypted format.
final case class SensitiveData[A](value: A) derives FastEq, Decoder, Encoder, SchemaFor {

  def encrypt(
    algorithm: SensitiveData.Algorithm,
    key:       SensitiveData.Key
  )(using Encoder[A], SchemaFor[A]): SensitiveData.Encrypted[A] = algorithm.encrypt[A](this, key)

  override def toString: String = "SENSITIVE DATA"
}
object SensitiveData {

  type Encrypted[A] = Encrypted.Type[A]
  object Encrypted extends NewtypeT[ArraySeq[Byte]] {

    extension [A](enc: Encrypted[A]) {
      def decrypt(
        algorithm: SensitiveData.Algorithm,
        key:       SensitiveData.Key
      )(using Decoder[A], SchemaFor[A]): DeserializationResult[SensitiveData[A]] =
        algorithm.decrypt[A](enc, key)
    }

    given [A]: Decoder[Encrypted[A]] = unsafeMakeF[Decoder, A](Decoder[Array[Byte]].map(ArraySeq.from))
    given [A]: Encoder[Encrypted[A]] =
      unsafeMakeF[Encoder, A](Encoder[Array[Byte]].contramap[ArraySeq[Byte]](_.toArray))
    given [A]: SchemaFor[Encrypted[A]] = SchemaFor[Array[Byte]].forType[Encrypted[A]]

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

    // Avro4s fails to correctly serialize-then-deserialize primitives so we have to use wrapper (GenericRecord schema)

    final def encrypt[A: Encoder: SchemaFor](value: SensitiveData[A], key: Key): Encrypted[A] = this match {
      case Blowfish =>
        val cipher = blowfishCipher(key, Cipher.ENCRYPT_MODE)
        AvroSerialization.serializeUnsafe(value).pipe(cipher.doFinal).pipe(ArraySeq.from(_)).pipe(Encrypted(_))
    }

    final def decrypt[A: Decoder: SchemaFor](
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
