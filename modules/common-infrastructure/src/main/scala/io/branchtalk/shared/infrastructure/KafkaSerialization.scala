package io.branchtalk.shared.infrastructure

import cats.effect.Sync
import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import fs2.kafka.{ Deserializer, Serializer }
import io.branchtalk.shared.model.AvroSerialization
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult

object KafkaSerialization {

  given [F[_]: Sync, A: AvroEncoder]: Serializer[F, A] =
    Serializer.lift[F, A](AvroSerialization.serialize(_))

  given [F[_]: Sync, A: AvroDecoder]: SafeDeserializer[F, A] =
    Deserializer.lift[F, DeserializationResult[A]](AvroSerialization.deserialize(_))
}
