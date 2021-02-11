package io.branchtalk.configs

import cats.effect.{ ExitCode, Sync }
import com.monovore.decline._
import com.typesafe.config.{ Config, ConfigRenderOptions }
import io.branchtalk.shared.model.ShowPretty
import io.scalaland.catnip.Semi

@Semi(ShowPretty) final case class AppArguments(
  host:                      String = Defaults.host,
  port:                      Int = Defaults.port,
  runAPI:                    Boolean = Defaults.runAPI,
  runUsersProjections:       Boolean = Defaults.runUsersProjections,
  runDiscussionsProjections: Boolean = Defaults.runDiscussionsProjections
) {

  def isAnythingRun: Boolean = runAPI || runUsersProjections || runDiscussionsProjections
}
object AppArguments {

  implicit private class BoolOps[A](private val opts: Opts[A]) extends AnyVal {

    def orBool(bool: Boolean)(implicit isUnit: A <:< Unit): Opts[Boolean] =
      if (bool) opts.orTrue else opts.orFalse
  }

  final case class NoConfig(help: Help) extends Exception {

    // scalastyle:off regex
    def printHelp(config: Config): ExitCode = {
      println(help.toString())
      println(additionalInfo(config))
      ExitCode.Success
    }

    def printError(): ExitCode = {
      println("Invalid arguments:")
      println(help.errors.map("  " + _).intercalate("\n"))
      ExitCode.Error
    }
    // scalastyle:on regex
  }

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

  def parse[F[_]: Sync](args: List[String], env: Map[String, String]): F[AppArguments] =
    Sync[F]
      .delay {
        Command(name = "branchtalk", header = "Starts backend server with selected services running") {
          (host, port, monolith orElse (runApi, runUsersProjections, runDiscussionsProjections).tupled).mapN {
            case (host, port, (runApi, runUsersProjections, runDiscussionsProjections)) =>
              AppArguments(
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

  private def logVariables = List(
    ("BRANCHTALK_CONTEXT_NAME", "branchtalk-monolith", "define context name (e.g. instance ID) used in each log"),
    ("DEFAULT_LOG_LEVEL", "INFO", "log level of everything not overridden by settings below"),
    ("BRANCHTALK_LOG_LEVEL", "INFO", "default log level of app's own logic"),
    ("BRANCHTALK_API_LOG_LEVEL", "$BRANCHTALK_LOG_LEVEL", "overrides level for API and HTTP related logs"),
    ("BRANCHTALK_INFRA_LOG_LEVEL", "$BRANCHTALK_LOG_LEVEL", "overrides level for reads- and writes-relates logs"),
    ("HTTP4S_LOG_LEVEL", "ERROR", "Http4s server log level"),
    ("FLYWAY_LOG_LEVEL", "ERROR", "Flyway migration log level"),
    ("HIKARI_LOG_LEVEL", "ERROR", "Hikari DB connection log level"),
    ("KAFKA_LOG_LEVEL", "ERROR", "Kafka log level")
  ).view
    .map { case (variable, default, description) =>
      s"""    $variable (default: $default)
         |        $description""".stripMargin
    }
    .mkString("\n")

  private val configRenderOptions =
    ConfigRenderOptions.defaults().setOriginComments(false).setFormatted(true).setJson(false)

  private def additionalInfo(config: Config): String =
    s"""
       |
       |Logback variables (overridable using -DVARIABLE_NAME=level JVM parameter):
       |$logVariables
       |
       |
       |HOCON configurations (overridable with -Dpath.to.config=value JVM parameter if there is no env var,
       |or through comma-separated list of files -Dconfig.overrides=config1.conf,config2.json):
       |${config.root.render(configRenderOptions)}""".stripMargin
}
