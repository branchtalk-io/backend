package io.branchtalk.shared.infrastructure

import io.branchtalk.shared.model.ShowPretty
import io.scalaland.catnip.Semi
import pureconfig.ConfigReader

@Semi(ConfigReader, ShowPretty) final case class KafkaEventConsumerConfig(
  consumerGroup: ConsumerGroup,
  maxCommitSize: MaxCommitSize,
  maxCommitTime: MaxCommitTime
)
