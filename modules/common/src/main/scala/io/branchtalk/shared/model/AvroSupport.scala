package io.branchtalk.shared.model

import java.net.URI
import java.util

import cats.Id
import cats.data.{ NonEmptyList, NonEmptyVector }
import com.sksamuel.avro4s._
import eu.timepit.refined.api.{ RefType, Validate }
import io.estatico.newtype.Coercible
import org.apache.avro.Schema

import scala.jdk.CollectionConverters._

object AvroSupport {

  // newtype - order of implicits is necessary (swapping them would break derivations, so we can't use typeclass syntax)

  implicit def coercibleDecoder[R, N](implicit ev: Coercible[R, N], R: Decoder[R]): Decoder[N] =
    Coercible.unsafeWrapMM[Decoder, Id, R, N].apply(R)
  implicit def coercibleEncoder[R, N](implicit ev: Coercible[R, N], R: Encoder[R]): Encoder[N] =
    Coercible.unsafeWrapMM[Encoder, Id, R, N].apply(R)
  implicit def coercibleSchemaFor[R, N](implicit ev: Coercible[R, N], R: SchemaFor[R]): SchemaFor[N] =
    Coercible.unsafeWrapMM[SchemaFor, Id, R, N].apply(R)

  // refined (redirections)

  implicit def refinedSchemaFor[T: SchemaFor, P, F[_, _]]: SchemaFor[F[T, P]] =
    refined.refinedSchemaFor[T, P, F]

  implicit def refinedEncoder[T: Encoder, P, F[_, _]: RefType]: Encoder[F[T, P]] =
    refined.refinedEncoder[T, P, F]

  implicit def refinedDecoder[T: Decoder, P: Validate[T, *], F[_, _]: RefType]: Decoder[F[T, P]] =
    refined.refinedDecoder[T, P, F]

  // cats (copy-paste as cats module isn't released)

  implicit def nonEmptyListSchemaFor[T: SchemaFor]: SchemaFor[NonEmptyList[T]] =
    SchemaFor(Schema.createArray(SchemaFor[T].schema))

  implicit def nonEmptyVectorSchemaFor[T: SchemaFor]: SchemaFor[NonEmptyVector[T]] =
    SchemaFor(Schema.createArray(SchemaFor[T].schema))

  implicit def nonEmptyListEncoder[T: Encoder]: Encoder[NonEmptyList[T]] =
    new Encoder[NonEmptyList[T]] {

      val schemaFor: SchemaFor[NonEmptyList[T]] = nonEmptyListSchemaFor(Encoder[T].schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Null"))
      override def encode(ts: NonEmptyList[T]): java.util.List[AnyRef] = {
        require(schema != null)
        ts.map(Encoder[T].encode).toList.asJava
      }
    }

  implicit def nonEmptyVectorEncoder[T: Encoder]: Encoder[NonEmptyVector[T]] =
    new Encoder[NonEmptyVector[T]] {

      val schemaFor: SchemaFor[NonEmptyVector[T]] = nonEmptyVectorSchemaFor(Encoder[T].schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.Equals", "org.wartremover.warts.Null"))
      override def encode(ts: NonEmptyVector[T]): java.util.List[AnyRef] = {
        require(schema != null)
        ts.map(Encoder[T].encode).toVector.asJava
      }
    }

  implicit def nonEmptyListDecoder[T: Decoder]: Decoder[NonEmptyList[T]] =
    new Decoder[NonEmptyList[T]] {

      val schemaFor: SchemaFor[NonEmptyList[T]] = nonEmptyListSchemaFor(Decoder[T].schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      override def decode(value: Any): NonEmptyList[T] = value match {
        case array: Array[_]           => NonEmptyList.fromListUnsafe(array.toList.map(Decoder[T].decode))
        case list:  util.Collection[_] => NonEmptyList.fromListUnsafe(list.asScala.map(Decoder[T].decode).toList)
        case other => sys.error("Unsupported type " + other.toString)
      }
    }

  implicit def nonEmptyVectorDecoder[T: Decoder]: Decoder[NonEmptyVector[T]] =
    new Decoder[NonEmptyVector[T]] {

      val schemaFor: SchemaFor[NonEmptyVector[T]] = nonEmptyVectorSchemaFor(Decoder[T].schemaFor)

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      override def decode(value: Any): NonEmptyVector[T] = value match {
        case array: Array[_]           => NonEmptyVector.fromVectorUnsafe(array.toVector.map(Decoder[T].decode))
        case list:  util.Collection[_] => NonEmptyVector.fromVectorUnsafe(list.asScala.map(Decoder[T].decode).toVector)
        case other => sys.error("Unsupported type " + other.toString)
      }
    }

  // custom types

  implicit val uriSchema: SchemaFor[URI] = SchemaFor[String].forType[URI]
  implicit val uriEncoder: Encoder[URI] = new Encoder[URI] {
    override def encode(value: URI): AnyRef = Encoder[String].encode(value.toString)
    override def schemaFor: SchemaFor[URI] = uriSchema
  }
  implicit val uriDecoder: Decoder[URI] = new Decoder[URI] {
    override def decode(value: Any): URI = URI.create(Decoder[String].decode(value))
    override def schemaFor: SchemaFor[URI] = uriSchema
  }
}
