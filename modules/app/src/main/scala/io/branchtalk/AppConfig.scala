package io.branchtalk

import cats.effect.Sync
import com.monovore.decline._

final case class AppConfig(
  host:                      String  = "localhost",
  port:                      Int     = 8080,
  runApi:                    Boolean = false,
  runDiscussionsProjections: Boolean = false
)
object AppConfig {

  final case class NoConfig(help: Help) extends Exception

  private val help: Opts[Nothing] =
    Opts.flag(long = "help", short = "?", help = "Show this information").asHelp

  private val host =
    Opts.option[String](long = "host", short = "h", help = "Set host address of this server").withDefault("localhost")
  private val port =
    Opts.option[Int](long = "port", short = "p", help = "Set port number of this server").withDefault(8080)

  private val monolith =
    Opts
      .flag(long  = "monolith",
            short = "M",
            help  = "Have this instance runApplication as monolith (all services enabled)")
      .map(_ => (true, true))
  private val runApi =
    Opts.flag(long = "api", short = "a", help = "Have this instance run application HTTP API").orFalse
  private val runDiscussionsProjections =
    Opts
      .flag(long  = "discussions-projections",
            short = "d",
            help  = "Have this instance runApplication Discussions write model projections")
      .orFalse

  def parse[F[_]: Sync](args: List[String], env: Map[String, String]): F[AppConfig] =
    Sync[F]
      .delay {
        Command(name = "branchtalk", header = "Starts backend server with selected services running") {
          (host, port, monolith orElse (runApi, runDiscussionsProjections).tupled).mapN {
            case (host, port, (runApi, runDiscussionsProjections)) =>
              AppConfig(host                      = host,
                        port                      = port,
                        runApi                    = runApi,
                        runDiscussionsProjections = runDiscussionsProjections)
          } orElse help

        }.parse(args, env)
      }
      .flatMap(result => Sync[F].fromEither(result.leftMap(NoConfig.apply)))
}
