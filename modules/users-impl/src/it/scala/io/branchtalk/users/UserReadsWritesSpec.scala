package io.branchtalk.users

import cats.effect.{ IO, Resource }
import io.branchtalk.{ IOTest, ResourcefulTest }
import io.branchtalk.shared.models.{ CommonError, ID, OptionUpdatable, UUIDGenerator, Updatable }
import io.branchtalk.users.model.{ Password, User }
import org.specs2.mutable.Specification

final class UserReadsWritesSpec extends Specification with IOTest with ResourcefulTest with UsersFixtures {

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

  "User Reads & Writes" should {

    "create a User and eventually read it" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          creationData <- (0 until 3).toList.traverse(_ => userCreate)
          // when
          toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
          ids = toCreate.map(_.id)
          users <- ids.traverse(usersReads.userReads.requireById).eventually()
          usersOpt <- ids.traverse(usersReads.userReads.getById).eventually()
          usersExist <- ids.traverse(usersReads.userReads.exists).eventually()
          userDeleted <- ids.traverse(usersReads.userReads.deleted).eventually()
        } yield {
          // then
          ids.toSet === users.map(_.id).toSet
          usersOpt.forall(_.isDefined) must beTrue
          usersExist.forall(identity) must beTrue
          userDeleted.exists(identity) must beFalse
        }
      }
    }

    "don't update a User that doesn't exists" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_.id)
          _ <- usersReads.userReads.requireById(moderatorID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => userCreate)
          fakeUpdateData <- creationData.traverse { data =>
            ID.create[IO, User].map { id =>
              User.Update(
                id                = id,
                moderatorID       = moderatorID.some,
                newUsername       = Updatable.Set(data.username),
                newDescription    = OptionUpdatable.setFromOption(data.description),
                newPassword       = Updatable.Set(data.password),
                updatePermissions = List.empty
              )
            }
          }
          // when
          toUpdate <- fakeUpdateData.traverse(usersWrites.userWrites.updateUser(_).attempt)
        } yield {
          // then
          toUpdate.forall(_.isLeft) must beTrue
        }
      }
    }

    "update an existing User" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_.id)
          _ <- usersReads.userReads.requireById(moderatorID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => userCreate)
          toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
          ids = toCreate.map(_.id)
          created <- ids.traverse(usersReads.userReads.requireById).eventually()
          updateData = created.zipWithIndex.collect {
            // TODO: change this data
            case (User(id, data), 0) =>
              User.Update(
                id                = id,
                moderatorID       = moderatorID.some,
                newUsername       = Updatable.Set(data.username),
                newDescription    = OptionUpdatable.setFromOption(data.description),
                newPassword       = Updatable.Set(data.password),
                updatePermissions = List.empty // TODO
              )
            case (User(id, _), 1) =>
              User.Update(
                id                = id,
                moderatorID       = moderatorID.some,
                newUsername       = Updatable.Keep,
                newDescription    = OptionUpdatable.Keep,
                newPassword       = Updatable.Keep,
                updatePermissions = List.empty // TODO
              )
            case (User(id, _), 2) =>
              User.Update(
                id                = id,
                moderatorID       = moderatorID.some,
                newUsername       = Updatable.Keep,
                newDescription    = OptionUpdatable.Erase,
                newPassword       = Updatable.Keep,
                updatePermissions = List.empty // TODO
              )
          }
          // when
          _ <- updateData.traverse(usersWrites.userWrites.updateUser)
          updated <- ids
            .traverse(usersReads.userReads.requireById)
            .flatTap { current =>
              IO(assert(current.last.data.lastModifiedAt.isDefined, "Updated entity should have lastModifiedAt set"))
            }
            .eventually()
        } yield {
          // then
          created
            .zip(updated)
            .zipWithIndex
            .collect {
              case ((User(_, older), User(_, newer)), 0) =>
                // set case
                older === newer.copy(lastModifiedAt = None)
              case ((User(_, older), User(_, newer)), 1) =>
                // keep case
                older === newer
              case ((User(_, older), User(_, newer)), 2) =>
                // erase case
                older.copy(description = None) === newer.copy(lastModifiedAt = None)
            }
            .forall(identity) must beTrue
        }
      }
    }

    "allow delete of a created User" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_.id)
          _ <- usersReads.userReads.requireById(moderatorID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => userCreate)
          // when
          toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
          ids = toCreate.map(_.id)
          _ <- ids.traverse(usersReads.userReads.requireById).eventually()
          _ <- ids.map(User.Delete(_, moderatorID.some)).traverse(usersWrites.userWrites.deleteUser)
          _ <- ids
            .traverse(usersReads.userReads.getById)
            .flatTap(results => IO(assert(results.forall(_.isEmpty), "All Users should be eventually deleted")))
            .eventually()
          notExist <- ids.traverse(usersReads.userReads.exists)
          areDeleted <- ids.traverse(usersReads.userReads.deleted)
        } yield {
          // then
          notExist.exists(identity) must beFalse
          areDeleted.forall(identity) must beTrue
        }
      }
    }

    "allow password checking" in {
      usersWrites.runProjector.use { projector =>
        for {
          // given
          _ <- projector.handleError(_.printStackTrace()).start
          goodPassword <- passwordCreate("password")
          userId <- userCreate.map(_.copy(password = goodPassword)).flatMap(usersWrites.userWrites.createUser).map(_.id)
          user <- usersReads.userReads.requireById(userId).eventually()
          // when
          ok <- usersReads.userReads
            .authenticate(user.data.username, Password.Raw.parse[IO]("password".getBytes).unsafeRunSync())
            .attempt
          fail <- usersReads.userReads
            .authenticate(user.data.username, Password.Raw.parse[IO]("bad".getBytes).unsafeRunSync())
            .attempt
        } yield {
          // then
          ok must beRight(user)
          fail must beLeft(anInstanceOf[CommonError.InvalidCredentials])
        }
      }
    }
  }
}
