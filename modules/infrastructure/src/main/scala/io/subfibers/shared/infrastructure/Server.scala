package io.subfibers.shared.infrastructure

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.types.string.NonEmptyString

final case class Server(
  host: NonEmptyString,
  port: Int Refined Positive
)
