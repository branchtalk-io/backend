package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import fs2.kafka.{ Deserializer, Serializer }
import io.branchtalk.shared.model.AvroSerialization
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult

object KafkaSerialization {

  implicit def kafkaSerializer[F[_]: Sync, A: Encoder]: Serializer[F, A] =
    Serializer.lift[F, A](AvroSerialization.serialize(_))

  implicit def kafkaDeserializer[F[_]: Sync, A: Decoder: SchemaFor]: SafeDeserializer[F, A] =
    Deserializer.lift[F, DeserializationResult[A]](AvroSerialization.deserialize(_))
}
