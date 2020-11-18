package io.branchtalk.users

import io.branchtalk.shared.model.{ CreationScheduled, TestUUIDGenerator }
import io.branchtalk.users.model.Session
import org.specs2.mutable.Specification

final class SessionReadsWritesSpec extends Specification with UsersIOTest with UsersFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Session Reads & Writes" should {

    "create a Session and immediately read it" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          userID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
          _ <- usersReads.userReads.requireById(userID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => sessionCreate(userID))
          // when
          toCreate <- creationData.traverse(usersWrites.sessionWrites.createSession)
          ids = toCreate.map(_.id)
          users <- ids.traverse(usersReads.sessionReads.requireSession)
        } yield
        // then
        ids must containTheSameElementsAs(users.map(_.id))
      }
    }

    "allow immediate delete of a created Session" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          userID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
          _ <- usersReads.userReads.requireById(userID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => sessionCreate(userID))
          toCreate <- creationData.traverse(usersWrites.sessionWrites.createSession)
          ids = toCreate.map(_.id)
          _ <- ids.traverse(usersReads.sessionReads.requireSession)
          // when
          _ <- ids.map(Session.Delete.apply).traverse(usersWrites.sessionWrites.deleteSession)
          sessions <- ids.traverse(usersReads.sessionReads.requireSession(_).attempt)
        } yield
        // then
        sessions must contain(beLeft[Throwable]).foreach
      }
    }

    "fetch Session created during registration" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          // when
          session <- usersReads.sessionReads.requireSession(sessionID).attempt
        } yield
        // then
        session must beRight[Session]
      }
    }
  }
}
