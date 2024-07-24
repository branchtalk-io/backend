package io.branchtalk.shared.model

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.net.URI
import java.util

import cats.{ Eq, Id, Show }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyVector }
import cats.effect.{ Resource, Sync, SyncIO }
import com.sksamuel.avro4s.*
import org.apache.avro.Schema
import neotype.*

import scala.collection.compat.Factory
import scala.jdk.CollectionConverters.*
import scala.util.{ Failure, Success }
import scala.util.control.NoStackTrace

import org.apache.avro.Schema

import scala.language.implicitConversions

// Missing helpers for serializations and deserialization with some API saner than byte array streams.
enum DeserializationError extends Throwable with NoStackTrace derives FastEq, ShowPretty {
  case DecodingError(badValue: String, error: Throwable)
}
object DeserializationError {

  private given Show[Throwable] = _.getMessage
  private given Eq[Throwable]   = _.getMessage === _.getMessage
}

object AvroSerialization {

  private val logger = com.typesafe.scalalogging.Logger(getClass)

  type DeserializationResult[+A] = Either[DeserializationError, A]

  def serialize[F[_]: Sync, A: Encoder: SchemaFor](value: A): F[Array[Byte]] =
    Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayOutputStream())).use { baos =>
      Sync[F].delay {
        val aos = AvroOutputStream.json[A].to(baos).build()
        aos.write(value)
        aos.close()
        aos.flush()
        baos.toByteArray
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Throw"))
  def deserialize[F[_]: Sync, A: Decoder: SchemaFor](arr: Array[Byte]): F[DeserializationResult[A]] =
    Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayInputStream(arr))).use { bais =>
      Sync[F]
        .delay {
          AvroInputStream
            .json[A]
            .from(bais)
            .build(SchemaFor[A].schema)
            .asInstanceOf[AvroJsonInputStream[A]]
            .singleEntity match {
            case Success(value) =>
              value.asRight[DeserializationError]
            case Failure(error) =>
              DeserializationError.DecodingError("Failed to extract Avro message", error).asLeft[A]
          }
        }
        .handleError { (error: Throwable) =>
          logger.error(s"Avro deserialization error for '${new String(arr, branchtalkCharset)}'", error)
          DeserializationError.DecodingError(new String(arr, branchtalkCharset), error).asLeft[A]
        }
    }

  def serializeUnsafe[A: Encoder: SchemaFor](value: A): Array[Byte] =
    serialize[SyncIO, A](value).unsafeRunSync()

  def deserializeUnsafe[A: Decoder: SchemaFor](arr: Array[Byte]): DeserializationResult[A] =
    deserialize[SyncIO, A](arr).unsafeRunSync()
}

object AvroSupport {

  // newtype - order of implicits is necessary (swapping them would break derivations, so we can't use typeclass syntax)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  given [A, B](using A: Newtype.WithType[B, A], B: Decoder[B]): Decoder[A] =
    B.map[A](b => A.make(b).fold[A](str => throw Avro4sDecodingException(str, b), identity[A]))
  given [A, B](using A: Newtype.WithType[B, A], B: Encoder[B]):   Encoder[A]   = B.contramap[A](A.unwrap)
  given [A, B](using A: Newtype.WithType[B, A], B: SchemaFor[B]): SchemaFor[A] = B.forType[A]

  // cats - copy pased because:
  // Implementation restriction: package com.sksamuel.avro4s.cats is not a valid prefix for a wildcard export, as it is a package

  import scala.jdk.CollectionConverters.*

  given [T](using schemaFor: SchemaFor[T]): SchemaFor[NonEmptyList[T]] = SchemaFor(Schema.createArray(schemaFor.schema))
  given [T](using schemaFor: SchemaFor[T]): SchemaFor[NonEmptyVector[T]] = SchemaFor(
    Schema.createArray(schemaFor.schema)
  )
  given [T](using schemaFor: SchemaFor[T]): SchemaFor[NonEmptyChain[T]] = SchemaFor(
    Schema.createArray(schemaFor.schema)
  )

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  given [T](using encoder: Encoder[T]): Encoder[NonEmptyList[T]] = (schema: Schema) => {
    require(schema.getType == Schema.Type.ARRAY)
    val encode = encoder.encode(schema)
    { value => value.map(encode).toList.asJava }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  given [T](using encoder: Encoder[T]): Encoder[NonEmptyVector[T]] = (schema: Schema) => {
    require(schema.getType == Schema.Type.ARRAY)
    val encode = encoder.encode(schema)
    { value => value.map(encode).toVector.asJava }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  given [T](using encoder: Encoder[T]): Encoder[NonEmptyChain[T]] = (schema: Schema) => {
    require(schema.getType == Schema.Type.ARRAY)
    val encode = encoder.encode(schema)
    { value => value.map(encode).toNonEmptyList.toList.asJava }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  given [T](using decoder: Decoder[T]): Decoder[NonEmptyList[T]] = (schema: Schema) => {
    require(schema.getType == Schema.Type.ARRAY)
    val decode = decoder.decode(schema)
    {
      case array: Array[?] if array.nonEmpty => NonEmptyList.fromListUnsafe(array.toList.map(decode))
      case list:  java.util.Collection[?] if !list.isEmpty =>
        NonEmptyList.fromListUnsafe(list.asScala.map(decode).toList)
      case other => sys.error(s"Unsupported type $other")
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  given [T](using decoder: Decoder[T]): Decoder[NonEmptyVector[T]] = (schema: Schema) => {
    require(schema.getType == Schema.Type.ARRAY)
    val decode = decoder.decode(schema)
    {
      case array: Array[?] if array.nonEmpty => NonEmptyVector.fromVectorUnsafe(array.toVector.map(decode))
      case list:  java.util.Collection[?] if !list.isEmpty =>
        NonEmptyVector.fromVectorUnsafe(list.asScala.map(decode).toVector)
      case other => sys.error(s"Unsupported type $other") // A
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.OptionPartial"))
  given [T](using decoder: Decoder[T]): Decoder[NonEmptyChain[T]] = (schema: Schema) => {
    require(schema.getType == Schema.Type.ARRAY)
    val decode = decoder.decode(schema)
    {
      case array: Array[?] if array.nonEmpty => NonEmptyChain.fromChainUnsafe(Chain.fromSeq(array.toSeq).map(decode))
      case list:  java.util.Collection[?] if !list.isEmpty =>
        NonEmptyChain.fromChainUnsafe(Chain.fromSeq(list.asScala.toSeq).map(decode))
      case other => sys.error(s"Unsupported type $other")
    }
  }

  // custom types

  given Decoder[URI] = Decoder[String].map(URI.create)
  @SuppressWarnings(Array("org.wartremover.warts.ToString")) // false warning - URI overrides toString
  given Encoder[URI]   = Encoder[String].contramap[URI](_.toString)
  given SchemaFor[URI] = SchemaFor[String].forType[URI]
}
