package io.branchtalk.discussions.api

import cats.effect.IO
import io.branchtalk.api.{ Authentication, Pagination, PaginationLimit, PaginationOffset, ServerIOTest }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.ChannelModels._
import io.branchtalk.discussions.model.Channel
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.scalaland.chimney.dsl._
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class ChannelServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  sequential // Channel pagination tests cannot be run in parallel to other Channel tests

  implicit protected lazy val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "ChannelServer-provided endpoints" should {

    "on GET /discussions/channels" in {

      "return newest Channels" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
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

    "on POST /discussions/channels" in {

      "create a new Channel" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              creationData <- channelCreate
              // when
              response <- ChannelAPIs.create.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
                creationData.transformInto[CreateChannelRequest]
              )
              // TODO: check that this creates a new channel eventually!
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(anInstanceOf[CreateChannelResponse]))
            }
        }
      }
    }

    "on GET /discussions/channels/{channelID}" in {

      "fetch existing Channel" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              channel <- discussionsReads.channelReads.requireById(channelID).eventually()
              // when
              response <- ChannelAPIs.read.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)).some,
                channelID
              )
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(be_===(APIChannel.fromDomain(channel))))
            }
        }
      }
    }

    "on PUT /discussions/channels/{channelID}" in {

      "update existing Channel when User is allowed to do it" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              channel <- discussionsReads.channelReads.requireById(channelID).eventually()
              newUrlName <- Channel.UrlName.parse[IO]("new-name")
              newName <- Channel.Name.parse[IO]("new name")
              newDescription <- Channel.Description.parse[IO]("lorem ipsum")
              // when
              response <- ChannelAPIs.update.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
                channelID,
                UpdateChannelRequest(
                  newUrlName = Updatable.Set(newUrlName),
                  newName = Updatable.Set(newName),
                  newDescription = OptionUpdatable.Set(newDescription)
                )
              )
              updatedChannel <- discussionsReads.channelReads
                .requireById(channelID)
                .assert("Updated entity should have lastModifiedAt set")(_.data.lastModifiedAt.isDefined)
                .eventually()
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(be_===(UpdateChannelResponse(channelID))))
              updatedChannel must_=== channel
                .lens(_.data.urlName)
                .set(newUrlName)
                .lens(_.data.name)
                .set(newName)
                .lens(_.data.description)
                .set(newDescription.some)
                .lens(_.data.lastModifiedAt)
                .set(updatedChannel.data.lastModifiedAt)
            }
        }
      }
    }

    "on DELETE /discussions/channels/{channelID}" in {

      "delete existing Channel when User is allowed to do it" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              // when
              response <- ChannelAPIs.delete.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
                channelID
              )
              _ <- discussionsReads.channelReads
                .deleted(channelID)
                .assert("Channel should be eventually deleted")(identity)
                .eventually()
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(be_===(DeleteChannelResponse(channelID))))
            }
        }
      }
    }

    "on POST /discussions/channels/{channelID}/restore" in {

      "restore deleted Channel when User is allowed to do it" in {
        (usersWrites.runProjector, discussionsWrites.runProjector).tupled.use {
          case (usersProjector, discussionsProjector) =>
            for {
              // given
              _ <- usersProjector.logError("Error reported by Users projector").start
              _ <- discussionsProjector.logError("Error reported by Discussions projector").start
              (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
                usersWrites.userWrites.createUser
              )
              _ <- usersReads.userReads.requireById(userID).eventually()
              _ <- usersReads.sessionReads.requireSession(sessionID).eventually()
              CreationScheduled(channelID) <- channelCreate.flatMap(discussionsWrites.channelWrites.createChannel)
              _ <- discussionsReads.channelReads.requireById(channelID).eventually()
              _ <- discussionsWrites.channelWrites.deleteChannel(
                Channel.Delete(channelID, userIDUsers2Discussions.get(userID))
              )
              _ <- discussionsReads.channelReads.requireById(channelID, isDeleted = true).eventually()
              // when
              response <- ChannelAPIs.restore.toTestCall.untupled(
                Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(sessionID)),
                channelID
              )
              _ <- discussionsReads.channelReads
                .exists(channelID)
                .assert("Channel should be eventually restored")(identity)
                .eventually()
            } yield {
              // then
              response.code must_=== StatusCode.Ok
              response.body must beValid(beRight(be_===(RestoreChannelResponse(channelID))))
            }
        }
      }
    }
  }
}
