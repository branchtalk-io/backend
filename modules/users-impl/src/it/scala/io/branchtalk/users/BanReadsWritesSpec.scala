package io.branchtalk.users

import cats.syntax.eq._
import io.branchtalk.shared.model.TestUUIDGenerator
import io.branchtalk.users.model.Ban
import io.branchtalk.users.model.BanProperties.Scope
import io.scalaland.chimney.dsl._
import org.specs2.mutable.Specification

final class BanReadsWritesSpec extends Specification with UsersIOTest with UsersFixtures {

  implicit protected val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "Ban Reads & Writes" should {

    "order a User's Ban and lift User's ban and eventually execute command" in {
      for {
        // given
        userID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
        _ <- usersReads.userReads.requireById(userID).eventually()
        channelID <- channelIDCreate
        expectedBans <- banCreate(userID, channelID).map(ban => List(ban, ban.copy(scope = Ban.Scope.Globally)))
        orderBanData = expectedBans.map(_.into[Ban.Order].withFieldConst(_.moderatorID, None).transform)
        liftBanData  = expectedBans.map(_.into[Ban.Lift].withFieldConst(_.moderatorID, None).transform)
        // when
        _ <- orderBanData.traverse(usersWrites.banWrites.orderBan)
        bansExecuted <- usersReads.banReads
          .findForUser(userID)
          .assert("Ban orders should be eventually executed")(_.size eqv 2)
          .eventually()
        channelBansExecuted <- usersReads.banReads.findForChannel(channelID)
        _ <- liftBanData.traverse(usersWrites.banWrites.liftBan)
        bansLifted <- usersReads.banReads
          .findForUser(userID)
          .assert("Ban lifts should be eventually executed")(b => expectedBans.forall(!b.contains(_)))
          .eventually()
      } yield {
        // then
        bansExecuted must_=== expectedBans.toSet
        channelBansExecuted must_=== expectedBans
          .filter(_.scope match {
            case Scope.ForChannel(_) => true
            case Scope.Globally      => false
          })
          .toSet
        bansLifted must beEmpty
      }
    }
  }
}
