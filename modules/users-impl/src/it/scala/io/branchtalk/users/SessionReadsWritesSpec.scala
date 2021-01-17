package io.branchtalk.users

import io.branchtalk.shared.model.{ CreationScheduled, TestUUIDGenerator }
import io.branchtalk.users.model.Session
import org.specs2.mutable.Specification

final class SessionReadsWritesSpec extends Specification with UsersIOTest with UsersFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Session Reads & Writes" should {

    "create a Session and immediately read it" in {
      for {
        // given
        userID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
        _ <- usersReads.userReads.requireById(userID).eventually()
        creationData <- (0 until 3).toList.traverse(_ => sessionCreate(userID))
        // when
        toCreate <- creationData.traverse(usersWrites.sessionWrites.createSession)
        ids = toCreate.map(_.id)
        users <- ids.traverse(usersReads.sessionReads.requireById)
      } yield
      // then
      ids must containTheSameElementsAs(users.map(_.id))
    }

    "allow immediate delete of a created Session" in {
      for {
        // given
        userID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
        _ <- usersReads.userReads.requireById(userID).eventually()
        creationData <- (0 until 3).toList.traverse(_ => sessionCreate(userID))
        toCreate <- creationData.traverse(usersWrites.sessionWrites.createSession)
        ids = toCreate.map(_.id)
        _ <- ids.traverse(usersReads.sessionReads.requireById)
        // when
        _ <- ids.map(Session.Delete.apply).traverse(usersWrites.sessionWrites.deleteSession)
        sessions <- ids.traverse(usersReads.sessionReads.requireById(_).attempt)
      } yield
      // then
      sessions must contain(beLeft[Throwable]).foreach
    }

    "fetch Session created during registration" in {
      for {
        // given
        (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
          usersWrites.userWrites.createUser
        )
        _ <- usersReads.userReads.requireById(userID).eventually()
        // when
        session <- usersReads.sessionReads.requireById(sessionID).attempt
      } yield
      // then
      session must beRight[Session]
    }

    "paginate Sessions" in {
      for {
        // given
        goodPassword <- passwordCreate("password")
        (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate
          .map(_.copy(password = goodPassword))
          .flatMap(
            usersWrites.userWrites.createUser
          )
        _ <- usersReads.userReads.requireById(userID).eventually()
        paginatedData <- (0 until 19).toList.traverse(_ => sessionCreate(userID))
        _ <- paginatedData.traverse(usersWrites.sessionWrites.createSession).map(_.map(_.id))
        // when
        pagination <- usersReads.sessionReads.paginate(userID, Session.Sorting.ClosestToExpiry, 0L, 10)
        pagination2 <- usersReads.sessionReads.paginate(userID, Session.Sorting.ClosestToExpiry, 10L, 10)
      } yield {
        // then
        pagination.entities must haveSize(10)
        pagination.nextOffset.map(_.value) must beSome(10)
        pagination2.entities must haveSize(10)
        pagination2.nextOffset.map(_.value) must beNone
      }
    }
  }
}
