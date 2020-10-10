package io.branchtalk.shared.infrastructure

import cats.data.NonEmptyList
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._
import pureconfig.ConfigReader
import pureconfig.module.cats._

@Semi(ConfigReader) final case class TestKafkaEventBusConfig(
  servers:     NonEmptyList[Server],
  topicPrefix: Topic
) {

  def topic(generatedSuffix: String): Topic =
    Topic(refineV[NonEmpty](topicPrefix.nonEmptyString.value + generatedSuffix).getOrElse(???))

  def toKafkaEventBusConfig(generatedSuffix: String): KafkaEventBusConfig =
    this.into[KafkaEventBusConfig].withFieldConst(_.topic, topic(generatedSuffix)).transform
}
