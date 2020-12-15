package io.branchtalk.shared.infrastructure

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import cats.effect.{ Resource, Sync }
import com.sksamuel.avro4s.{ AvroInputStream, AvroOutputStream, Decoder, Encoder, SchemaFor }
import com.typesafe.scalalogging.Logger
import fs2.kafka.{ Deserializer, Serializer }
import io.branchtalk.ADT
import io.branchtalk.shared.model.branchtalkCharset

sealed trait DeserializationError extends ADT
object DeserializationError {
  case object NoAvroMessage extends DeserializationError
  final case class DecodingError(badValue: String, error: Throwable) extends DeserializationError
}

object KafkaSerialization {

  private val logger = Logger(getClass)

  implicit def kafkaSerializer[F[_]: Sync, A: Encoder]: Serializer[F, A] = Serializer.lift[F, A] { a =>
    Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayOutputStream())).use { baos =>
      Sync[F].delay {
        val aos = AvroOutputStream.json[A].to(baos).build()
        aos.write(a)
        aos.close()
        aos.flush()
        baos.toByteArray
      }
    }
  }

  implicit def kafkaDeserializer[F[_]: Sync, A: Decoder: SchemaFor]: SafeDeserializer[F, A] =
    Deserializer.lift[F, DeserializationError Either A] { arr =>
      Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayInputStream(arr))).use { bais =>
        Sync[F]
          .delay {
            AvroInputStream.json[A].from(bais).build(SchemaFor[A].schema).iterator.nextOption() match {
              case Some(value) =>
                value.asRight[DeserializationError]
              case None =>
                logger.error(s"No Avro message to deserialize for '${new String(arr, branchtalkCharset)}'")
                DeserializationError.NoAvroMessage.asLeft[A]
            }
          }
          .handleError { error: Throwable =>
            logger.error(s"Avro deserialization error for '${new String(arr, branchtalkCharset)}'", error)
            DeserializationError.DecodingError(new String(arr, branchtalkCharset), error).asLeft[A]
          }
      }
    }
}
