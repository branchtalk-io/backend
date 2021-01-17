package io.branchtalk.shared.model

import cats.{ Eq, Show }
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import enumeratum._
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import io.scalaland.chimney._
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import scala.collection.compat.immutable.ArraySeq
import scala.util.{ Random, Try }

final case class SensitiveData[A](value: A) {

  def encode(
    algorithm:        SensitiveData.Algorithm,
    key:              SensitiveData.Key
  )(implicit encoder: Encoder[A]): SensitiveData.Encrypted[A] = algorithm.encrypt[A](value, key)

  override def toString: String = "SENSITIVE DATA"
}
object SensitiveData {

  @newtype final case class Encrypted[A](bytes: ArraySeq[Byte]) {

    def decrypt(
      algorithm:        SensitiveData.Algorithm,
      key:              SensitiveData.Key
    )(implicit decoder: Decoder[A], schemaFor: SchemaFor[A]): DeserializationResult[A] =
      algorithm.decrypt[A](this, key)
  }
  object Encrypted {

    implicit def show[A]: Show[Encrypted[A]] = Show.wrap(_ => "ENCRYPTED")
    implicit def eq[A]:   Eq[Encrypted[A]]   = Eq.by(_.bytes)

    implicit def decoder[A]: Decoder[Encrypted[A]] =
      Decoder[Array[Byte]].map(ArraySeq.from(_)).coerce[Decoder[Encrypted[A]]]
    implicit def encoder[A]: Encoder[Encrypted[A]] =
      Encoder[Array[Byte]].comap[ArraySeq[Byte]](_.toArray).coerce[Encoder[Encrypted[A]]]
    implicit def schemaFor[A]: SchemaFor[Encrypted[A]] =
      SchemaFor[Array[Byte]].forType[ArraySeq[Byte]].coerce[SchemaFor[Encrypted[A]]]
  }

  @newtype final case class Key(bytes: ArraySeq[Byte])
  object Key {

    implicit val show: Show[Key] = Show.wrap(_ => "KEY")
    implicit val eq:   Eq[Key]   = Eq.by(_.bytes)
  }

  sealed trait Algorithm extends EnumEntry with EnumEntry.Hyphencase {

    def generateKey(): Key

    def encrypt[A: Encoder](value: A, key: Key): Encrypted[A]

    def decrypt[A: Decoder: SchemaFor](encrypted: Encrypted[A], key: Key): DeserializationResult[A]
  }
  object Algorithm extends Enum[Algorithm] {

    case object Blowfish extends Algorithm {

      private val defaultKeySize = 32

      override def generateKey(): Key = Key(ArraySeq.from(Random.nextBytes(defaultKeySize)))

      override def encrypt[A: Encoder](value: A, key: Key): Encrypted[A] = {
        val spec   = new SecretKeySpec(key.bytes.toArray, "Blowfish")
        val cipher = Cipher.getInstance("Blowfish").tap(_.init(Cipher.ENCRYPT_MODE, spec))
        AvroSerialization.serializeUnsafe(value).pipe(cipher.doFinal).pipe(ArraySeq.from(_)).pipe(Encrypted(_))
      }

      override def decrypt[A: Decoder: SchemaFor](encrypted: Encrypted[A], key: Key): DeserializationResult[A] = {
        val spec   = new SecretKeySpec(key.bytes.toArray, "Blowfish")
        val cipher = Cipher.getInstance("Blowfish").tap(_.init(Cipher.DECRYPT_MODE, spec))
        Try(cipher.doFinal(encrypted.bytes.toArray)).toEither.left
          .map(DeserializationError.DecodingError("SensitiveData decoding error", _))
          .flatMap(AvroSerialization.deserializeUnsafe[A](_))
      }
    }

    def default: Algorithm = Blowfish // TODO: make configurable

    val values = findValues
  }

  implicit def encryptionTransformer[A](implicit
    algorithm: Algorithm,
    key:       Key,
    encoder:   Encoder[A]
  ): Transformer[SensitiveData[A], SensitiveData.Encrypted[A]] = _.encode(algorithm, key)

  implicit def decryptionTransformer[A](implicit
    algorithm: Algorithm,
    key:       Key,
    decoder:   Decoder[A],
    schemaFor: SchemaFor[A]
  ): TransformerF[DeserializationResult, SensitiveData.Encrypted[A], SensitiveData[A]] =
    _.decrypt(algorithm, key).map(SensitiveData(_))

  implicit def show[A]: Show[A] = Show.wrap(_ => "SENSITIVE DATA")
  implicit def eq[A: Eq]: Eq[SensitiveData[A]] = Eq.by(_.value)
}
