package io.branchtalk.users

import cats.effect.{ IO, Resource }
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.shared.model.UUID

trait UsersIOTest extends IOTest, ResourcefulTest {

  protected given uuidGenerator: UUID.Generator

  // populated by resources
  protected var usersCfg:    DomainModule.Config = _
  protected var usersReads:  UsersReads[IO]      = _
  protected var usersWrites: UsersWrites[IO]     = _

  protected lazy val usersResource: Resource[IO, Unit] = for {
    _ <- TestUsersConfig.loadDomainConfig[IO].map(usersCfg = _)
    _ <- UsersModule.reads[IO](usersCfg, registry).map(usersReads = _)
    _ <- UsersModule.writes[IO](usersCfg, registry).map(usersWrites = _)
    _ <- usersWrites.runProjections.asResource
  } yield ()

  override protected def testResource: Resource[IO, Unit] = super.testResource >> usersResource
}
