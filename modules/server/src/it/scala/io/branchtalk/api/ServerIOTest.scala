package io.branchtalk.api

import cats.effect.{ IO, Resource }
import io.branchtalk.discussions.DiscussionsIOTest
import io.branchtalk.users.{ UsersIOTest, UsersModule }
import org.http4s.server.Server
import org.specs2.matcher.{ OptionLikeCheckedMatcher, OptionLikeMatcher, ValueCheck }
import sttp.client3.{ Response, SttpBackend }
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.Uri
import sttp.tapir._
import sttp.tapir.client.sttp._

trait ServerIOTest extends UsersIOTest with DiscussionsIOTest {

  // populated by resources
  protected var server: Server               = _
  protected var client: SttpBackend[IO, Any] = _
  protected lazy val sttpBaseUri: Uri = Uri.unsafeApply(
    scheme = server.baseUri.scheme.fold(???)(_.value),
    host = server.baseUri.host.fold(???)(_.value),
    port = server.baseUri.port.fold(???)(_.intValue())
  )

  protected lazy val serverResource: Resource[IO, Unit] = for {
    _ <- UsersModule
      .listenToUsers(usersCfg)(discussionsReads.discussionEventConsumer, usersWrites.runDiscussionsConsumer)
      .asResource
    (appArguments, apiConfig) <- TestApiConfigs.asResource[IO]
    _ <- AppServer
      .asResource[IO](
        appArguments = appArguments,
        apiConfig = apiConfig,
        registry = registry,
        userReads = usersReads.userReads,
        sessionReads = usersReads.sessionReads,
        banReads = usersReads.banReads,
        userWrites = usersWrites.userWrites,
        sessionWrites = usersWrites.sessionWrites,
        banWrites = usersWrites.banWrites,
        channelReads = discussionsReads.channelReads,
        postReads = discussionsReads.postReads,
        commentReads = discussionsReads.commentReads,
        subscriptionReads = discussionsReads.subscriptionReads,
        commentWrites = discussionsWrites.commentWrites,
        postWrites = discussionsWrites.postWrites,
        channelWrites = discussionsWrites.channelWrites,
        subscriptionWrites = discussionsWrites.subscriptionWrites
      )
      .map(server = _)
    _ <- AsyncHttpClientCatsBackend.resource[IO]().map(client = _)
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> serverResource

  implicit class ServerTestOps[I, E, O](private val endpoint: Endpoint[Unit, I, E, O, Any]) {

    val toTestCall: I => IO[Response[DecodeResult[Either[E, O]]]] = (input: I) =>
      SttpClientInterpreter(SttpClientOptions.default)
        .toRequest(
          endpoint,
          sttpBaseUri.some
        )
        .apply(input)
        .acceptEncoding("deflate")
        .send(client)
  }

  implicit class AuthServerTestOps[A, I, E, O](private val authEndpoint: AuthedEndpoint[A, I, E, O, Any]) {

    val toTestCall: (A, I) => IO[Response[DecodeResult[Either[E, O]]]] = (auth: A, input: I) =>
      SttpClientInterpreter(SttpClientOptions.default)
        .toSecureRequest(
          authEndpoint.endpoint,
          sttpBaseUri.some
        )
        .apply(auth)
        .apply(input)
        .acceptEncoding("deflate")
        .send(client)
  }

  implicit class AuthOnlyServerTestOps[A, E, O](private val authEndpoint: AuthedEndpoint[A, Unit, E, O, Any]) {

    val toTestCall: A => IO[Response[DecodeResult[Either[E, O]]]] = (auth: A) =>
      SttpClientInterpreter(SttpClientOptions.default)
        .toSecureRequest(
          authEndpoint.endpoint,
          sttpBaseUri.some
        )
        .apply(auth)
        .apply(())
        .acceptEncoding("deflate")
        .send(client)
  }

  import ServerIOTest._

  implicit def toDecoderResultOps[A](result: DecodeResult[A]): DecodeResultOps[A] = new DecodeResultOps[A](result)

  import org.specs2.control.ImplicitParameters._
  def beValid[T](t:          ValueCheck[T]):                     ValidResultCheckedMatcher[T] = ValidResultCheckedMatcher(t)
  def beValid[T](implicit p: ImplicitParam = implicitParameter): ValidResultMatcher[T]        = use(p)(ValidResultMatcher[T]())
}

object ServerIOTest {

  implicit class DecodeResultOps[A](private val result: DecodeResult[A]) extends AnyVal {

    def toValidOpt: Option[A] = result match {
      case DecodeResult.Value(t) => t.some
      case _                     => none[A]
    }
  }

  final case class ValidResultMatcher[T]()
      extends OptionLikeMatcher[DecodeResult, T, T]("DecodeResult.Value", (_: DecodeResult[T]).toValidOpt)

  final case class ValidResultCheckedMatcher[T](check: ValueCheck[T])
      extends OptionLikeCheckedMatcher[DecodeResult, T, T]("DecodeResult.Value", (_: DecodeResult[T]).toValidOpt, check)
}
