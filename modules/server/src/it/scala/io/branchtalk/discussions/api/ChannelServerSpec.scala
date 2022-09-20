package io.branchtalk.discussions.api

import cats.effect.IO
import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, _ }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.ChannelModels._
import io.branchtalk.discussions.model.Channel
import io.branchtalk.mappings._
import io.branchtalk.shared.model._
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.model.{ Permission, RequiredPermissions }
import io.scalaland.chimney.dsl._
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class ChannelServerSpec extends Specification with ServerIOTest with UsersFixtures with DiscussionsFixtures {

  implicit protected lazy val uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "ChannelServer-provided endpoints" should {

    // GET (pagination) moved to a separate test suite

    "on POST /discussions/channels" in {

      "create a new Channel" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
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

    "on GET /discussions/channels/{channelID}" in {

      "fetch existing Channel" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
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

    "on PUT /discussions/channels/{channelID}" in {

      "update existing Channel when User is allowed to do it" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate
            .map(_.focus(_.authorID).replace(userIDUsers2Discussions.get(userID))) // make User Channels' owner
            .flatMap(discussionsWrites.channelWrites.createChannel)
          channel <- discussionsReads.channelReads.requireById(channelID).eventually()
          _ <- usersReads.userReads
            .requireById(userID)
            .assert("User should eventually become own's Channel Moderator")(
              _.data.permissions.allow(
                RequiredPermissions.one(Permission.ModerateChannel(channelIDUsers2Discussions.reverseGet(channelID)))
              )
            )
            .eventually()
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
            .focus(_.data.urlName)
            .replace(newUrlName)
            .focus(_.data.name)
            .replace(newName)
            .focus(_.data.description)
            .replace(newDescription.some)
            .focus(_.data.lastModifiedAt)
            .replace(updatedChannel.data.lastModifiedAt)
        }
      }
    }

    "on DELETE /discussions/channels/{channelID}" in {

      "delete existing Channel when User is allowed to do it" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate
            .map(_.focus(_.authorID).replace(userIDUsers2Discussions.get(userID))) // make User Channels' owner
            .flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          _ <- usersReads.userReads
            .requireById(userID)
            .assert("User should eventually become own's Channel Moderator")(
              _.data.permissions.allow(
                RequiredPermissions.one(Permission.ModerateChannel(channelIDUsers2Discussions.reverseGet(channelID)))
              )
            )
            .eventually()
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

    "on POST /discussions/channels/{channelID}/restore" in {

      "restore deleted Channel when User is allowed to do it" in {
        for {
          // given
          (CreationScheduled(userID), CreationScheduled(sessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(userID).eventually()
          _ <- usersReads.sessionReads.requireById(sessionID).eventually()
          CreationScheduled(channelID) <- channelCreate
            .map(_.focus(_.authorID).replace(userIDUsers2Discussions.get(userID))) // make User Channels' owner
            .flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          _ <- usersReads.userReads
            .requireById(userID)
            .assert("User should eventually become own's Channel Moderator")(
              _.data.permissions.allow(
                RequiredPermissions.one(Permission.ModerateChannel(channelIDUsers2Discussions.reverseGet(channelID)))
              )
            )
            .eventually()
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
