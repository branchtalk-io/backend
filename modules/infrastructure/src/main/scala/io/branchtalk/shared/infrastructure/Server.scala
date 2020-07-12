package io.branchtalk.shared.infrastructure

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.string.NonEmptyString
import io.scalaland.catnip.Semi
import pureconfig._

@Semi(ConfigReader) final case class Server(
  host: NonEmptyString,
  port: Int Refined Positive
)
