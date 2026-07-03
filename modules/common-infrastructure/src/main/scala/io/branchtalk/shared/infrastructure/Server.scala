package io.branchtalk.shared.infrastructure

import cats.Show
import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }

final case class Server(
  host: Server.Host,
  port: Server.Port
) derives ConfigReader
object Server {

  type Host = Host.Type
  object Host extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(host: Host): Some[String] = Some(host.unwrap)

    given ConfigReader[Host] = summon[ConfigReader[String]].emapString("Host")(make)
    given Show[Host]         = unsafeMakeF[Show](Show[String])
  }

  type Port = Port.Type
  object Port extends Newtype[Int] {

    override inline def validate(input: Int): Boolean = input > 0

    def unapply(port: Port): Some[Int] = Some(port.unwrap)

    given ConfigReader[Port] = summon[ConfigReader[Int]].emapString("Port")(make)
    given Show[Port]         = unsafeMakeF[Show](Show[Int])
  }

  given Show[Server] = (s: Server) => show"${s.host}:${s.port}"
}
