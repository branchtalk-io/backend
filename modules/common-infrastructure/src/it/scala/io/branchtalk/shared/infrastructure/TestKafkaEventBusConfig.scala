package io.branchtalk.shared.infrastructure

import cats.data.NonEmptyList
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._

@Semi(ConfigReader) final case class TestKafkaEventBusConfig(
  servers:       NonEmptyList[Server],
  topicPrefix:   Topic,
  cache:         Server
) {

  def topic(generatedSuffix: String): Topic =
    Topic(refineV[NonEmpty](topicPrefix.nonEmptyString.value + generatedSuffix).getOrElse(???))

  def toKafkaEventBusConfig(generatedSuffix: String): KafkaEventBusConfig =
    this.into[KafkaEventBusConfig].withFieldConst(_.topic, topic(generatedSuffix)).transform
}
