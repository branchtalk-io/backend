package io.branchtalk.shared.infrastructure

import io.scalaland.catnip.Semi
import pureconfig.ConfigReader

@Semi(ConfigReader) final case class KafkaEventConsumerConfig(
  consumerGroup: ConsumerGroup,
  maxCommitSize: MaxCommitSize,
  maxCommitTime: MaxCommitTime
)
