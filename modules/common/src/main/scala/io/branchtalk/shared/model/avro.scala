package io.branchtalk.shared.model

import java.net.URI
import java.time.OffsetDateTime
import cats.{ Eq, Show }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyVector }
import cats.effect.Sync
import hearth.kindlings.avroderivation.{ AvroConfig, AvroDecoder, AvroEncoder, AvroIO, AvroSchemaFor }
import io.scalaland.chimney.partial
import org.apache.avro.Schema
import neotype.*

import scala.jdk.CollectionConverters.*
import scala.util.Try
import scala.util.control.NoStackTrace

// Default Avro derivation config, in scope wherever the model is imported so `derives AvroEncoder, AvroDecoder` works.
given AvroConfig = AvroConfig.default

// Missing helpers for serializations and deserialization with some API saner than byte array streams.
enum DeserializationError extends Throwable, NoStackTrace derives FastEq, ShowPretty {
  case DecodingError(badValue: String, error: Throwable)
  case DecryptionError(badValue: String, errors: Map[String, String])
}
object DeserializationError {

  private given Show[Throwable] = _.getMessage
  private given Eq[Throwable]   = _.getMessage === _.getMessage

  given partial.AsResult[AvroSerialization.DeserializationResult] with {
    def asResult[A](fa: AvroSerialization.DeserializationResult[A]): partial.Result[A] =
      fa.fold[partial.Result[A]](partial.Result.fromErrorThrowable, partial.Result.fromValue)
  }
}

object AvroSerialization {

  private val logger = com.typesafe.scalalogging.Logger(getClass)

  type DeserializationResult[+A] = Either[DeserializationError, A]

  def serialize[F[_]: Sync, A: AvroEncoder](value: A): F[Array[Byte]] =
    Sync[F].delay(AvroIO.toBinary(value))

  def deserialize[F[_]: Sync, A: AvroDecoder](arr: Array[Byte]): F[DeserializationResult[A]] =
    Sync[F].delay(AvroIO.fromBinary[A](arr).asRight[DeserializationError]).handleError { (error: Throwable) =>
      logger.error(s"Avro deserialization error for '${new String(arr, branchtalkCharset)}'", error)
      DeserializationError.DecodingError(new String(arr, branchtalkCharset), error).asLeft[A]
    }

  def serializeUnsafe[A: AvroEncoder](value: A): Array[Byte] =
    AvroIO.toBinary(value)

  def deserializeUnsafe[A: AvroDecoder](arr: Array[Byte]): DeserializationResult[A] =
    Try(AvroIO.fromBinary[A](arr)).toEither.left
      .map(DeserializationError.DecodingError(new String(arr, branchtalkCharset), _))
}

// A combined codec: AvroEncoder and AvroDecoder both extend AvroSchemaFor, so providing them as SEPARATE givens makes
// `summon[AvroSchemaFor[T]]` ambiguous (both qualify). One AvroCodec given per supported "leaf" type means each of the
// three summons (encoder / decoder / schema) resolves to exactly one instance.
trait AvroCodec[A] extends AvroEncoder[A], AvroDecoder[A]

// Base-type codecs Kindlings doesn't provide out of the box (encoded as ISO / canonical strings). Top-level so they
// are in scope wherever `io.branchtalk.shared.model.*` is imported (e.g. newtype companions building `newtypeCodec`).

@SuppressWarnings(Array("org.wartremover.warts.ToString")) // false warning - URI overrides toString
given AvroCodec[URI] with {
  private val E = summon[AvroEncoder[String]]
  private val D = summon[AvroDecoder[String]]
  def schema:             Schema = E.schema
  def encode(value: URI): Any    = E.encode(value.toString)
  def decode(value: Any): URI    = URI.create(D.decode(value))
}

given AvroCodec[UUID] with {
  private val E = summon[AvroEncoder[String]]
  private val D = summon[AvroDecoder[String]]
  def schema:              Schema = E.schema
  def encode(value: UUID): Any    = E.encode(value.toString)
  def decode(value: Any):  UUID   = java.util.UUID.fromString(D.decode(value))
}

given AvroCodec[OffsetDateTime] with {
  private val E = summon[AvroEncoder[String]]
  private val D = summon[AvroDecoder[String]]
  def schema:                        Schema         = E.schema
  def encode(value: OffsetDateTime): Any            = E.encode(value.toString)
  def decode(value: Any):            OffsetDateTime = OffsetDateTime.parse(D.decode(value))
}

given AvroCodec[Array[Byte]] with {
  private val E = summon[AvroEncoder[scala.collection.immutable.ArraySeq[Byte]]]
  private val D = summon[AvroDecoder[scala.collection.immutable.ArraySeq[Byte]]]
  def schema:                     Schema      = E.schema
  def encode(value: Array[Byte]): Any         = E.encode(scala.collection.immutable.ArraySeq.unsafeWrapArray(value))
  def decode(value: Any):         Array[Byte] = D.decode(value).toArray
}

object AvroSupport {

  private def arraySchema(elem: Schema): Schema = Schema.createArray(elem)

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  private def decodeArray[T](decoder: AvroDecoder[T], value: Any): List[T] = value match {
    case array: Array[?]                => array.toList.map(decoder.decode)
    case list:  java.util.Collection[?] => list.asScala.map(decoder.decode).toList
    case other => sys.error(s"Unsupported type $other")
  }

  // newtype - wrap the underlying type's instances.
  // For newtypes defined in the SAME module as the derived event, Kindlings' derivation macro cannot summon the
  // generic given below (its `Newtype.WithType` evidence is a same-run transparent-inline given the macro can't see),
  // so such newtypes must expose a concrete `given AvroCodec[X] = AvroSupport.newtypeCodec` in their companion.
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def newtypeCodec[B, A](using A: Newtype.WithType[B, A], E: AvroEncoder[B], D: AvroDecoder[B]): AvroCodec[A] =
    new AvroCodec[A] {
      def schema:           Schema = E.schema
      def encode(value: A): Any    = E.encode(A.unwrap(value))
      def decode(value: Any): A =
        A.make(D.decode(value)).fold[A](str => throw new IllegalArgumentException(str), identity[A])
    }

  given [A, B](using Newtype.WithType[B, A], AvroEncoder[B], AvroDecoder[B]): AvroCodec[A] = newtypeCodec[B, A]

  // Combine an encoder + decoder into a single AvroCodec (so `summon[AvroSchemaFor[A]]` stays unambiguous). Useful to
  // give a concrete instance to a type - e.g. an enum used inside `Updatable[...]` - via `avroCodec(using
  // AvroEncoder.derived, AvroDecoder.derived)`.
  def avroCodec[A](using E: AvroEncoder[A], D: AvroDecoder[A]): AvroCodec[A] = new AvroCodec[A] {
    def schema:             Schema = E.schema
    def encode(value: A):   Any    = E.encode(value)
    def decode(value: Any): A      = D.decode(value)
  }

  // Updatable: Kindlings' structural avro derivation mis-names the generic-enum union records (encoder writes
  // `Set__Type`, decoder expects `Set`) when the enum is the sole such field of a record, so events fail to
  // round-trip. Encode as the isomorphic Avro union [null, A] (Set(a) -> a, Keep -> null) built directly from the
  // element codec, sidestepping the buggy derivation.
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  given [A](using E: AvroEncoder[A], D: AvroDecoder[A]): AvroCodec[Updatable[A]] with {
    def schema: Schema = Schema.createUnion(Schema.create(Schema.Type.NULL), E.schema)
    def encode(value: Updatable[A]): Any = value match {
      case Updatable.Set(a) => E.encode(a)
      case Updatable.Keep   => null
    }
    def decode(value: Any): Updatable[A] = value match {
      case null => Updatable.Keep
      case v    => Updatable.Set(D.decode(v))
    }
  }

  // cats - non-empty collections encoded as Avro arrays

  given [T](using E: AvroEncoder[T], D: AvroDecoder[T]): AvroCodec[NonEmptyList[T]] with {
    def schema:                         Schema          = arraySchema(E.schema)
    def encode(value: NonEmptyList[T]): Any             = value.map(E.encode).toList.asJava
    def decode(value: Any):             NonEmptyList[T] = NonEmptyList.fromListUnsafe(decodeArray(D, value))
  }
  given [T](using E: AvroEncoder[T], D: AvroDecoder[T]): AvroCodec[NonEmptyVector[T]] with {
    def schema:                           Schema = arraySchema(E.schema)
    def encode(value: NonEmptyVector[T]): Any    = value.map(E.encode).toVector.asJava
    def decode(value: Any): NonEmptyVector[T] = NonEmptyVector.fromVectorUnsafe(decodeArray(D, value).toVector)
  }
  given [T](using E: AvroEncoder[T], D: AvroDecoder[T]): AvroCodec[NonEmptyChain[T]] with {
    def schema:                          Schema = arraySchema(E.schema)
    def encode(value: NonEmptyChain[T]): Any    = value.map(E.encode).toNonEmptyList.toList.asJava
    def decode(value: Any): NonEmptyChain[T] = NonEmptyChain.fromChainUnsafe(Chain.fromSeq(decodeArray(D, value)))
  }

  // custom types

}
