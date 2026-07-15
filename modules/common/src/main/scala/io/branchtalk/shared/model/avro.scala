package io.branchtalk.shared.model

import java.net.URI
import java.time.OffsetDateTime
import cats.{ Eq, Show }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyVector }
import cats.effect.Sync
import hearth.kindlings.avroderivation.{ AvroConfig, AvroDecoder, AvroEncoder, AvroIO, AvroSchemaFor }
import io.scalaland.chimney.partial
import org.apache.avro.Schema

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

object AvroSupport {

  // Base-type codecs Kindlings doesn't provide out of the box (encoded as ISO / canonical strings). They live here (not
  // top-level) so that the `import AvroSupport.{ *, given }` present at every avro-derivation site brings them into
  // scope - a bare `import io.branchtalk.shared.model.*` does NOT import givens under `-source 3.3-migration`, and the
  // newtype fields that need them are now unwrapped to their underlying at the derivation site by the neotype provider.

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

  private def arraySchema(elem: Schema): Schema = Schema.createArray(elem)

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  private def decodeArray[T](decoder: AvroDecoder[T], value: Any): List[T] = value match {
    case array: Array[?]                => array.toList.map(decoder.decode)
    case list:  java.util.Collection[?] => list.asScala.map(decoder.decode).toList
    case other => sys.error(s"Unsupported type $other")
  }

  // neotype `Newtype`/`Subtype` opaque types are handled by the `IsValueTypeProviderForNeotype` macro extension
  // (module `neotype-kindlings`, on the compile classpath): every Kindlings derivation unwraps them to their underlying
  // type automatically, so companions no longer need a per-type `given AvroCodec[X]`. (The generic
  // `Newtype.WithType`-based given that used to live here could never resolve inside the derivation macro anyway - see
  // that provider's docs.)

  // `Updatable[A]` / `OptionUpdatable[A]` no longer need a hand-written codec: the enclosing event records derive them
  // structurally (inline). Older Kindlings mis-named the generic-enum union records (encoder wrote `Set__Type`, decoder
  // expected `Set`), silently dropping events; fixed in Kindlings 0.3.1.

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
