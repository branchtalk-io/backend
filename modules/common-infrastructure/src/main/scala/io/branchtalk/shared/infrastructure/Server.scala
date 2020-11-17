package io.branchtalk.shared.infrastructure

import cats.Show
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.infrastructure.PureconfigSupport._
import io.scalaland.catnip.Semi

@Semi(ConfigReader) final case class Server(
  host: NonEmptyString,
  port: Int Refined Positive
)
object Server {
  implicit def show: Show[Server] = (s: Server) => s"${s.host.value}:${s.port.value.show}"
}
