package io.branchtalk.shared.infrastructure

import java.util

import cats.data.{ NonEmptyList, NonEmptyVector }
import com.sksamuel.avro4s._
import eu.timepit.refined.api.{ RefType, Validate }
import io.estatico.newtype.Coercible
import org.apache.avro.Schema

import scala.jdk.CollectionConverters._

object AvroSupport {

  // newtype

  implicit def coercibleSchemaFor[R, N](
    implicit ev: Coercible[SchemaFor[R], SchemaFor[N]],
    R:           SchemaFor[R]
  ): SchemaFor[N] =
    ev(R)

  // refined (redirections)

  implicit def refinedSchemaFor[T, P, F[_, _]](implicit schemaFor: SchemaFor[T]): SchemaFor[F[T, P]] =
    refined.refinedSchemaFor[T, P, F]

  implicit def refinedEncoder[T: Encoder, P, F[_, _]: RefType]: Encoder[F[T, P]] =
    refined.refinedEncoder[T, P, F]

  implicit def refinedDecoder[T: Decoder, P, F[_, _]: RefType](implicit validate: Validate[T, P]): Decoder[F[T, P]] =
    refined.refinedDecoder[T, P, F]

  // cats (copy-paste as cats module isn't released)

  implicit def nonEmptyListSchemaFor[T](implicit schemaFor: SchemaFor[T]): SchemaFor[NonEmptyList[T]] =
    SchemaFor(Schema.createArray(schemaFor.schema))

  implicit def nonEmptyVectorSchemaFor[T](implicit schemaFor: SchemaFor[T]): SchemaFor[NonEmptyVector[T]] =
    SchemaFor(Schema.createArray(schemaFor.schema))

  implicit def nonEmptyListEncoder[T](implicit encoder: Encoder[T]): Encoder[NonEmptyList[T]] =
    new Encoder[NonEmptyList[T]] {

      val schemaFor: SchemaFor[NonEmptyList[T]] = nonEmptyListSchemaFor(encoder.schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Null"))
      override def encode(ts: NonEmptyList[T]): java.util.List[AnyRef] = {
        require(schema != null)
        ts.map(encoder.encode).toList.asJava
      }
    }

  implicit def nonEmptyVectorEncoder[T](implicit encoder: Encoder[T]): Encoder[NonEmptyVector[T]] =
    new Encoder[NonEmptyVector[T]] {

      val schemaFor: SchemaFor[NonEmptyVector[T]] = nonEmptyVectorSchemaFor(encoder.schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Null"))
      override def encode(ts: NonEmptyVector[T]): java.util.List[AnyRef] = {
        require(schema != null)
        ts.map(encoder.encode).toVector.asJava
      }
    }

  implicit def nonEmptyListDecoder[T](implicit decoder: Decoder[T]): Decoder[NonEmptyList[T]] =
    new Decoder[NonEmptyList[T]] {

      val schemaFor: SchemaFor[NonEmptyList[T]] = nonEmptyListSchemaFor(decoder.schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      override def decode(value: Any): NonEmptyList[T] = value match {
        case array: Array[_]           => NonEmptyList.fromListUnsafe(array.toList.map(decoder.decode))
        case list:  util.Collection[_] => NonEmptyList.fromListUnsafe(list.asScala.map(decoder.decode).toList)
        case other => sys.error("Unsupported type " + other.toString)
      }
    }

  implicit def nonEmptyVectorDecoder[T](implicit decoder: Decoder[T]): Decoder[NonEmptyVector[T]] =
    new Decoder[NonEmptyVector[T]] {

      val schemaFor: SchemaFor[NonEmptyVector[T]] = nonEmptyVectorSchemaFor(decoder.schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      override def decode(value: Any): NonEmptyVector[T] = value match {
        case array: Array[_]           => NonEmptyVector.fromVectorUnsafe(array.toVector.map(decoder.decode))
        case list:  util.Collection[_] => NonEmptyVector.fromVectorUnsafe(list.asScala.map(decoder.decode).toVector)
        case other => sys.error("Unsupported type " + other.toString)
      }
    }
}
