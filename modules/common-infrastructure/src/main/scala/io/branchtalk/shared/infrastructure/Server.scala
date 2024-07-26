package io.branchtalk.shared.infrastructure

import cats.Show
import io.branchtalk.shared.infrastructure.PureconfigSupport.*
import neotype.*

final case class Server(
  host: Server.Host,
  port: Server.Port
) derives ConfigReader
object Server {

  type Host = Host.Type
  object Host extends Newtype[String] {

    override def validate(input: String): Boolean | String = input.nonEmpty

    def unapply(host: Host): Some[String] = Some(host.unwrap)

    given ConfigReader[Host] = ConfigReader[String].emapString("Host")(make)
    given Show[Host]         = unsafeMakeF[Show](Show[String])
  }

  type Port = Port.Type
  object Port extends Newtype[Int] {

    override def validate(input: Int): Boolean | String = input > 0

    def unapply(port: Port): Some[Int] = Some(port.unwrap)

    given ConfigReader[Port] = ConfigReader[Int].emapString("Port")(make)
    given Show[Port]         = unsafeMakeF[Show](Show[Int])
  }

  given Show[Server] = (s: Server) => show"${s.host}:${s.port}"
}
