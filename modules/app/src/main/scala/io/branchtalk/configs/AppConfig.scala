package io.branchtalk.configs

import cats.effect.Sync
import com.monovore.decline._

final case class AppConfig(
  host:                      String = Defaults.host,
  port:                      Int = Defaults.port,
  runAPI:                    Boolean = Defaults.runAPI,
  runUsersProjections:       Boolean = Defaults.runUsersProjections,
  runDiscussionsProjections: Boolean = Defaults.runDiscussionsProjections
)
object AppConfig {

  implicit private class BoolOps[A](private val opts: Opts[A]) extends AnyVal {

    def orBool(bool: Boolean)(implicit isUnit: A <:< Unit): Opts[Boolean] =
      if (bool) opts.orTrue else opts.orFalse
  }

  final case class NoConfig(help: Help) extends Exception

  private val help: Opts[Nothing] =
    Opts.flag(long = "help", short = "?", help = "Show this information").asHelp

  private val host =
    Opts.option[String](long = "host", short = "h", help = "Set host address of this server").withDefault(Defaults.host)
  private val port =
    Opts.option[Int](long = "port", short = "p", help = "Set port number of this server").withDefault(Defaults.port)

  private val monolith =
    Opts
      .flag(long = "monolith", short = "M", help = "Have this instance run as monolith (all services enabled)")
      .map(_ => (true, true, true))
  private val runApi =
    Opts.flag(long = "api", short = "a", help = "Have this instance run application HTTP API").orBool(Defaults.runAPI)
  private val runUsersProjections =
    Opts
      .flag(long = "users-projections", short = "u", help = "Have this instance run Users write model projections")
      .orBool(Defaults.runUsersProjections)
  private val runDiscussionsProjections =
    Opts
      .flag(long = "discussions-projections",
            short = "d",
            help = "Have this instance run Discussions write model projections"
      )
      .orBool(Defaults.runDiscussionsProjections)

  def parse[F[_]: Sync](args: List[String], env: Map[String, String]): F[AppConfig] =
    Sync[F]
      .delay {
        Command(name = "branchtalk", header = "Starts backend server with selected services running") {
          (host, port, monolith orElse (runApi, runUsersProjections, runDiscussionsProjections).tupled).mapN {
            case (host, port, (runApi, runUsersProjections, runDiscussionsProjections)) =>
              AppConfig(
                host = host,
                port = port,
                runAPI = runApi,
                runUsersProjections = runUsersProjections,
                runDiscussionsProjections = runDiscussionsProjections
              )
          } orElse help

        }.parse(args, env)
      }
      .flatMap(result => Sync[F].fromEither(result.leftMap(NoConfig.apply)))
}
