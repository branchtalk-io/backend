package io.branchtalk.discussions.api

import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, _ }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.ChannelModels._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class ChannelServerPaginationSpec
    extends Specification
    with ServerIOTest
    with UsersFixtures
    with DiscussionsFixtures {

  // Channel pagination tests cannot be run in parallel to other Channel tests (no parent to filter other tests)
  sequential

  implicit protected lazy val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "ChannelServer-provided pagination endpoints" should {

    "on GET /discussions/channels" in {

      "return newest Channels" in {
        for {
          // given
          channelIDs <- (0 until 10).toList.traverse(_ =>
            channelCreate.flatMap(discussionsWrites.channelWrites.createChannel).map(_.id)
          )
          channels <- channelIDs.traverse(discussionsReads.channelReads.requireById(_)).eventually()
          // when
          response1 <- ChannelAPIs.paginate.toTestCall.untupled(None, None, PaginationLimit(5).some)
          response2 <- ChannelAPIs.paginate.toTestCall.untupled(None,
                                                                PaginationOffset(5L).some,
                                                                PaginationLimit(5).some
          )
        } yield {
          // then
          response1.code must_=== StatusCode.Ok
          response1.body must beValid(beRight(anInstanceOf[Pagination[APIChannel]]))
          response2.code must_=== StatusCode.Ok
          response2.body must beValid(beRight(anInstanceOf[Pagination[APIChannel]]))
          (response1.body.toValidOpt.flatMap(_.toOption), response2.body.toValidOpt.flatMap(_.toOption))
            .mapN { (pagination1, pagination2) =>
              (pagination1.entities.toSet ++ pagination2.entities.toSet) must_=== channels
                .map(APIChannel.fromDomain)
                .toSet
            }
            .getOrElse(pass)
        }
      }
    }
  }
}
