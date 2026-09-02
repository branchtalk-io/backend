package io.branchtalk.api

import cats.effect.{ IO, Resource }
import io.branchtalk.discussions.DiscussionsIOTest
import io.branchtalk.notifications.{ NotificationsModule, TestNotificationsConfig }
import io.branchtalk.notifications.writes.NotificationTopic
import io.branchtalk.users.{ UsersIOTest, UsersModule }
import io.branchtalk.shared.infrastructure.*
import org.http4s.server.Server
import org.specs2.matcher.{ OptionLikeCheckedMatcher, OptionLikeMatcher, ValueCheck }
import sttp.client3.{ Response, SttpBackend }
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.Uri
import sttp.tapir.*
import sttp.tapir.client.sttp.*

trait ServerIOTest extends UsersIOTest, DiscussionsIOTest {

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
    notificationsCfg <- TestNotificationsConfig.loadDomainConfig[IO]
    notificationTopic <- NotificationTopic.create[IO]
    notificationsReads <- NotificationsModule.reads[IO](notificationsCfg, registry)
    notificationsWrites <- NotificationsModule.writes[IO](notificationsCfg, discussionsCfg, registry, notificationTopic)
    _ <- notificationsWrites.runProjections.asResource
    _ <- NotificationsModule
      .listenToDiscussions(notificationsCfg)(discussionsReads.discussionEventConsumer,
                                             notificationsWrites.runDiscussionsConsumer
      )
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
        subscriptionWrites = discussionsWrites.subscriptionWrites,
        notificationReads = notificationsReads.notificationReads,
        notificationWrites = notificationsWrites.notificationWrites,
        notificationTopic = notificationTopic
      )
      .map(server = _)
    _ <- AsyncHttpClientCatsBackend.resource[IO]().map(client = _)
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> serverResource

  extension [I, E, O](endpoint: Endpoint[Unit, I, E, O, Any]) {

    def toTestCall: I => IO[Response[DecodeResult[Either[E, O]]]] = (input: I) =>
      SttpClientInterpreter(SttpClientOptions.default)
        .toRequest(
          endpoint,
          sttpBaseUri.some
        )
        .apply(input)
        .acceptEncoding("deflate")
        .send(client)
  }

  extension [A, I, E, O](authEndpoint: AuthedEndpoint[A, I, E, O, Any]) {

    def toTestCall: (A, I) => IO[Response[DecodeResult[Either[E, O]]]] = (auth: A, input: I) =>
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

  extension [A, E, O](authEndpoint: AuthedEndpoint[A, Unit, E, O, Any]) {

    def toTestCall: A => IO[Response[DecodeResult[Either[E, O]]]] = (auth: A) =>
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

  import ServerIOTest.*
  export ServerIOTest.toValidOpt

  def beValid[T](t: ValueCheck[T]):    ValidResultCheckedMatcher[T] = ValidResultCheckedMatcher(t)
  def beValid[T](using DummyImplicit): ValidResultMatcher[T]        = ValidResultMatcher[T]()
}

object ServerIOTest {

  extension [A](result: DecodeResult[A]) {
    def toValidOpt: Option[A] = result match {
      case DecodeResult.Value(t) => t.some
      case _                     => none[A]
    }
  }

  final case class ValidResultMatcher[T]()
      extends OptionLikeMatcher[DecodeResult[T], T]("DecodeResult.Value", (_: DecodeResult[T]).toValidOpt)

  final case class ValidResultCheckedMatcher[T](check: ValueCheck[T])
      extends OptionLikeCheckedMatcher[DecodeResult[T], T]("DecodeResult.Value", (_: DecodeResult[T]).toValidOpt, check)
}
