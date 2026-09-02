package io.branchtalk.discussions.api

import cats.effect.IO
import com.softwaremill.quicklens.*
import io.branchtalk.api.{ Permission => _, RequiredPermissions => _, * }
import io.branchtalk.discussions.DiscussionsFixtures
import io.branchtalk.discussions.api.ChannelModels.*
import io.branchtalk.discussions.model.Channel
import io.branchtalk.mappings.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.infrastructure.*
import io.branchtalk.users.UsersFixtures
import io.branchtalk.users.model.{ Permission, RequiredPermissions }
import io.scalaland.chimney.dsl.*
import org.specs2.mutable.Specification
import sttp.model.StatusCode

final class ChannelServerSpec extends Specification, ServerIOTest, UsersFixtures, DiscussionsFixtures {

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

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
          createdChannelID = response.body.toValidOpt.flatMap(_.toOption).map(_.id)
          _ <- createdChannelID.traverse(channelID =>
            discussionsReads.channelReads
              .requireById(channelID)
              .assert("Created Channel should eventually appear on the read side")(
                _.data.urlName eqv creationData.urlName
              )
              .eventually()
          )
        } yield {
          // then
          response.code === StatusCode.Ok
          response.body must beValid(beRight(beAnInstanceOf[CreateChannelResponse]))
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(APIChannel.fromDomain(channel))))
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
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(userID))) // make User Channels' owner
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
          newUrlName <- ParseNewtype[IO].parse[Channel.UrlName]("new-name")
          newName <- ParseNewtype[IO].parse[Channel.Name]("new name")
          newDescription <- ParseNewtype[IO].parse[Channel.Description]("lorem ipsum")
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(UpdateChannelResponse(channelID))))
          updatedChannel === channel
            .modify(_.data.urlName)
            .setTo(newUrlName)
            .modify(_.data.name)
            .setTo(newName)
            .modify(_.data.description)
            .setTo(newDescription.some)
            .modify(_.data.lastModifiedAt)
            .setTo(updatedChannel.data.lastModifiedAt)
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
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(userID))) // make User Channels' owner
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(DeleteChannelResponse(channelID))))
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
            .map(_.modify(_.authorID).setTo(userIDUsers2Discussions.get(userID))) // make User Channels' owner
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
          response.code === StatusCode.Ok
          response.body must beValid(beRight(be_==(RestoreChannelResponse(channelID))))
        }
      }
    }

    // Negative-path tests

    "on POST /discussions/channels with invalid session" in {

      "return 401 Unauthorized for a non-existent session" in {
        for {
          // given
          creationData <- channelCreate
          fakeSessionID = SessionID(java.util.UUID.randomUUID())
          // when
          response <- ChannelAPIs.create.toTestCall.untupled(
            Authentication.Session(sessionID = fakeSessionID),
            creationData.transformInto[CreateChannelRequest]
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[ChannelError.BadCredentials]))
        }
      }
    }

    "on GET /discussions/channels/{channelID} for non-existent Channel" in {

      "return 404 Not Found" in {
        for {
          // given
          fakeChannelID <- ID.create[IO, Channel]
          // when
          response <- ChannelAPIs.read.toTestCall.untupled(None, fakeChannelID)
        } yield {
          // then
          response.code === StatusCode.NotFound
          response.body must beValid(beLeft(beAnInstanceOf[ChannelError.NotFound]))
        }
      }
    }

    "on PUT /discussions/channels/{channelID} by non-owner without moderator permission" in {

      "return 401 when User has no permission to update the Channel" in {
        for {
          // given
          (CreationScheduled(otherUserID), CreationScheduled(otherSessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(otherUserID).eventually()
          _ <- usersReads.sessionReads.requireById(otherSessionID).eventually()
          CreationScheduled(channelID) <- channelCreate // authorID is a random user, not otherUserID
            .flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          newName <- ParseNewtype[IO].parse[Channel.Name]("unauthorized update")
          // when
          response <- ChannelAPIs.update.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(otherSessionID)),
            channelID,
            UpdateChannelRequest(
              newUrlName = Updatable.Keep,
              newName = Updatable.Set(newName),
              newDescription = OptionUpdatable.Keep
            )
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[ChannelError.NoPermission]))
        }
      }
    }

    "on DELETE /discussions/channels/{channelID} by non-owner without moderator permission" in {

      "return 401 when User has no permission to delete the Channel" in {
        for {
          // given
          (CreationScheduled(otherUserID), CreationScheduled(otherSessionID)) <- userCreate.flatMap(
            usersWrites.userWrites.createUser
          )
          _ <- usersReads.userReads.requireById(otherUserID).eventually()
          _ <- usersReads.sessionReads.requireById(otherSessionID).eventually()
          CreationScheduled(channelID) <- channelCreate // authorID is a random user, not otherUserID
            .flatMap(discussionsWrites.channelWrites.createChannel)
          _ <- discussionsReads.channelReads.requireById(channelID).eventually()
          // when
          response <- ChannelAPIs.delete.toTestCall.untupled(
            Authentication.Session(sessionID = sessionIDApi2Users.reverseGet(otherSessionID)),
            channelID
          )
        } yield {
          // then
          response.code === StatusCode.Unauthorized
          response.body must beValid(beLeft(beAnInstanceOf[ChannelError.NoPermission]))
        }
      }
    }
  }
}
