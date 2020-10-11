package io.branchtalk.users

import cats.effect.{ IO, Resource }
import io.branchtalk.shared.models.UUIDGenerator
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.users.model.Session
import org.specs2.mutable.Specification

final class SessionReadsWritesSpec extends Specification with IOTest with ResourcefulTest with UsersFixtures {

  private implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  // populated by resources
  private var usersReads:  UsersReads[IO]  = _
  private var usersWrites: UsersWrites[IO] = _

  override protected def testResource: Resource[IO, Unit] =
    for {
      domainCfg <- TestUsersConfig.loadDomainConfig[IO]
      reads <- UsersModule.reads[IO](domainCfg)
      writes <- UsersModule.writes[IO](domainCfg)
    } yield {
      usersReads  = reads
      usersWrites = writes
    }

  "Session Reads & Writes" should {

    "create a Session and immediately read it" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          userID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
          _ <- usersReads.userReads.requireById(userID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => sessionCreate(userID))
          // when
          toCreate <- creationData.traverse(usersWrites.sessionWrites.createSession)
          ids = toCreate.map(_.id)
          users <- ids.traverse(usersReads.sessionReads.requireSession)
        } yield {
          // then
          ids.toSet === users.map(_.id).toSet
        }
      }
    }

    "allow immediate delete of a created Session" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          userID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
          _ <- usersReads.userReads.requireById(userID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => sessionCreate(userID))
          toCreate <- creationData.traverse(usersWrites.sessionWrites.createSession)
          ids = toCreate.map(_.id)
          _ <- ids.traverse(usersReads.sessionReads.requireSession)
          // when
          _ <- ids.map(Session.Delete.apply).traverse(usersWrites.sessionWrites.deleteSession)
          sessions <- ids.traverse(usersReads.sessionReads.requireSession(_).attempt)
        } yield {
          // then
          sessions.forall(_.isLeft) must beTrue
        }
      }
    }

    "fetch Session created during registration" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          scheduled <- userCreate.flatMap(usersWrites.userWrites.createUser)
          userID    = scheduled._1.id
          sessionID = scheduled._2.id
          _ <- usersReads.userReads.requireById(userID).eventually()
          // when
          session <- usersReads.sessionReads.requireSession(sessionID).attempt
        } yield {
          // then
          session must beRight[Session]
        }
      }
    }
  }
}
