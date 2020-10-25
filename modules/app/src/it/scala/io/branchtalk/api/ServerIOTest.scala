package io.branchtalk.api

import cats.effect.{ IO, Resource }
import io.branchtalk.discussions.DiscussionsIOTest
import io.branchtalk.users.UsersIOTest
import org.http4s.server.Server

trait ServerIOTest extends UsersIOTest with DiscussionsIOTest {

  // populated by resources
  protected var server: Server[IO] = _

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
      .map(server = _)
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> serverResource
}
