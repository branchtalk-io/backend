package io.branchtalk.users

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.shared.models.UUIDGenerator

trait UsersIOTest extends IOTest with ResourcefulTest {

  protected implicit val uuidGenerator: UUIDGenerator

  // populated by resources
  protected var usersReads:  UsersReads[IO]  = _
  protected var usersWrites: UsersWrites[IO] = _

  protected val usersResource: Resource[IO, Unit] = for {
    usersCfg <- TestUsersConfig.loadDomainConfig[IO]
    _ <- UsersModule.reads[IO](usersCfg).map(usersReads   = _)
    _ <- UsersModule.writes[IO](usersCfg).map(usersWrites = _)
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> usersResource
}
