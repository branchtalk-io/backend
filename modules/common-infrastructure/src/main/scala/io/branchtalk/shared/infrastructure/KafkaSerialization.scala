package io.branchtalk.shared.infrastructure

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import cats.effect.{ Resource, Sync }
import com.sksamuel.avro4s.{ AvroInputStream, AvroOutputStream, Decoder, Encoder }
import fs2.kafka.{ Deserializer, Serializer }

object KafkaSerialization {

  implicit def kafkaSerializer[F[_]: Sync, A: Encoder]: Serializer[F, A] = Serializer.instance[F, A] { (_, _, a) =>
    Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayOutputStream())).use { baos =>
      Sync[F].delay {
        AvroOutputStream.binary[A].to(baos).build().write(a)
        baos.toByteArray
      }
    }
  }

  implicit def kafkaDeserializer[F[_]: Sync, A: Decoder]: Deserializer[F, A] = Deserializer.instance[F, A] {
    (_, _, arr) =>
      Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayInputStream(arr))).use { bais =>
        Sync[F].delay {
          AvroInputStream.binary[A].from(bais).build.iterator.next()
        }
      }
  }
}
