package io.branchtalk.api

import cats.effect.{ IO, Resource }
import io.branchtalk.discussions.DiscussionsIOTest
import io.branchtalk.users.UsersIOTest
import org.http4s.server.Server
import org.specs2.matcher.{ OptionLikeCheckedMatcher, OptionLikeMatcher, ValueCheck }
import sttp.client.{ Response, SttpBackend }
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.Uri
import sttp.tapir._
import sttp.tapir.client.sttp._

trait ServerIOTest extends UsersIOTest with DiscussionsIOTest {

  // populated by resources
  protected var server: Server[IO]                                 = _
  protected var client: SttpBackend[IO, Nothing, WebSocketHandler] = _
  protected lazy val sttpBaseUri = Uri.unsafeApply(
    scheme = server.baseUri.scheme.fold(???)(_.value),
    host   = server.baseUri.host.fold(???)(_.value),
    port   = server.baseUri.port.fold(???)(_.intValue())
  )

  protected val serverResource: Resource[IO, Unit] = for {
    (appConfig, apiConfig) <- TestApiConfigs.asResource[IO]
    _ <- AppServer
      .asResource[IO](
        appConfig         = appConfig,
        apiConfig         = apiConfig,
        usersReads        = usersReads,
        usersWrites       = usersWrites,
        discussionsReads  = discussionsReads,
        discussionsWrites = discussionsWrites
      )
      .map(server                                             = _)
    _ <- AsyncHttpClientCatsBackend.resource[IO]().map(client = _)
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> serverResource

  implicit class ServerTestOps[I, E, O](private val endpoint: Endpoint[I, E, O, Nothing]) {

    def toTestCall(input: I): IO[Response[DecodeResult[Either[E, O]]]] =
      endpoint.toSttpRequest(sttpBaseUri).apply(input).send[IO]()(backend = client, isIdInRequest = implicitly)
  }

  import org.specs2.control.ImplicitParameters._
  def beValid[T](t:          ValueCheck[T]): ValidResultCheckedMatcher[T] = ValidResultCheckedMatcher(t)
  def beValid[T](implicit p: ImplicitParam = implicitParameter): ValidResultMatcher[T] = use(p)(ValidResultMatcher[T]())
}

final case class ValidResultMatcher[T]()
    extends OptionLikeMatcher[DecodeResult, T, T](
      "DecodeResult.Value",
      (_: DecodeResult[T]).pipe {
        case DecodeResult.Value(t) => t.some
        case _                     => none[T]
      }
    )

final case class ValidResultCheckedMatcher[T](check: ValueCheck[T])
    extends OptionLikeCheckedMatcher[DecodeResult, T, T](
      "DecodeResult.Value",
      (_: DecodeResult[T]).pipe {
        case DecodeResult.Value(t) => t.some
        case _                     => none[T]
      },
      check
    )
