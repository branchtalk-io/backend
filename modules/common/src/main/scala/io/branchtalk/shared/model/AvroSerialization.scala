package io.branchtalk.shared.model

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import cats.{ Eq, Show }
import cats.effect.{ IO, Resource, Sync }
import com.sksamuel.avro4s._
import io.branchtalk.ADT
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult
import io.scalaland.catnip.Semi
import io.scalaland.chimney.TransformerFSupport

import scala.collection.compat.Factory
import scala.util.{ Failure, Success }

@Semi(FastEq, ShowPretty) sealed trait DeserializationError extends ADT
object DeserializationError {
  final case class DecodingError(badValue: String, error: Throwable) extends DeserializationError

  implicit val eitherTransformerSupport: TransformerFSupport[DeserializationResult] =
    new TransformerFSupport[DeserializationResult] {

      override def pure[A](value: A): DeserializationResult[A] = value.asRight

      override def product[A, B](
        fa: DeserializationResult[A],
        fb: => DeserializationResult[B]
      ): DeserializationResult[(A, B)] = for { a <- fa; b <- fb } yield (a, b)

      override def map[A, B](fa: DeserializationResult[A], f: A => B): DeserializationResult[B] = fa.map(f)

      override def traverse[M, A, B](it: Iterator[A], f: A => DeserializationResult[B])(implicit
        fac:                             Factory[B, M]
      ): DeserializationResult[M] = it.toList.traverse(f).map(fac.fromSpecific)
    }

  implicit private def showThrowable: Show[Throwable] = _.getMessage
  implicit private def eqThrowable:   Eq[Throwable]   = _.getMessage === _.getMessage
}

object AvroSerialization {

  private val logger = com.typesafe.scalalogging.Logger(getClass)

  type DeserializationResult[+A] = Either[DeserializationError, A]

  def serialize[F[_]: Sync, A: Encoder](value: A): F[Array[Byte]] =
    Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayOutputStream())).use { baos =>
      Sync[F].delay {
        val aos = AvroOutputStream.json[A].to(baos).build()
        aos.write(value)
        aos.close()
        aos.flush()
        baos.toByteArray
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
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
        .handleError { error: Throwable =>
          logger.error(s"Avro deserialization error for '${new String(arr, branchtalkCharset)}'", error)
          DeserializationError.DecodingError(new String(arr, branchtalkCharset), error).asLeft[A]
        }
    }

  def serializeUnsafe[A: Encoder](value: A): Array[Byte] = serialize[IO, A](value).unsafeRunSync()

  def deserializeUnsafe[A: Decoder: SchemaFor](arr: Array[Byte]): DeserializationResult[A] =
    deserialize[IO, A](arr).unsafeRunSync()
}
