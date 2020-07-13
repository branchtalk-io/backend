package io.branchtalk

import cats.effect.Sync
import cats.implicits._
import com.monovore.decline._

final case class Arguments(
  runApi:                    Boolean = false,
  runDiscussionsProjections: Boolean = false
)
object Arguments {

  final case class ParsingError(help: Help) extends Exception

  private val monolith =
    Opts.flag(long = "monolith", short = "M", help = "Have this instance run as monolith (all services enabled)")
  private val runApi =
    Opts.flag(long = "api", short = "a", help = "Have this instance run API")
  private val runDiscussionsWrites =
    Opts.flag(long  = "discussions-projections",
              short = "d",
              help  = "Have this instance run Discussions write model projections")

  def parse[F[_]: Sync](args: List[String], env: Map[String, String]): F[Arguments] =
    Sync[F]
      .delay {
        Command(name = "branchtalk", header = "Starts backend server with selected services runnings") {
          monolith.map(_ => Arguments(runApi = true, runDiscussionsProjections = true)).orElse {
            (runApi.orFalse, runDiscussionsWrites.orFalse).mapN(Arguments.apply)
          }
        }.parse(args, env)
      }
      .flatMap(result => Sync[F].fromEither(result.leftMap(ParsingError.apply)))
}
