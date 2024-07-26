package io.branchtalk.shared.infrastructure

import fs2.kafka.Deserializer
import io.branchtalk.shared.model.AvroSerialization.DeserializationResult

type SafeDeserializer[F[_], Event] = Deserializer[F, DeserializationResult[Event]]
object SafeDeserializer {

  inline def apply[F[_], Event](using sd: SafeDeserializer[F, Event]): SafeDeserializer[F, Event] = sd
}
