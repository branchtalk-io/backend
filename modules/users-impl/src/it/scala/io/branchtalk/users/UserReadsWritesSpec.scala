package io.branchtalk.users

import cats.effect.IO
import io.branchtalk.shared.models.{ CommonError, ID, OptionUpdatable, UUIDGenerator, Updatable }
import io.branchtalk.users.model.{ Password, Permission, Permissions, User }
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification

final class UserReadsWritesSpec extends Specification with UsersIOTest with UsersFixtures {

  protected implicit val uuidGenerator: UUIDGenerator = UUIDGenerator.FastUUIDGenerator

  "User Reads & Writes" should {

    "create a User and eventually read it" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          creationData <- (0 until 3).toList.traverse(_ => userCreate)
          // when
          toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
          ids = toCreate.map(_._1.id)
          users <- ids.traverse(usersReads.userReads.requireById).eventually()
          usersOpt <- ids.traverse(usersReads.userReads.getById).eventually()
          usersExist <- ids.traverse(usersReads.userReads.exists).eventually()
          userDeleted <- ids.traverse(usersReads.userReads.deleted).eventually()
        } yield {
          // then
          ids must containTheSameElementsAs(users.map(_.id))
          usersOpt must contain(beSome[User]).foreach
          usersExist must contain(beTrue).foreach
          userDeleted must not(contain(beTrue).atLeastOnce)
        }
      }
    }

    "don't update a User that doesn't exists" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
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
          toUpdate must contain(beLeft[Throwable]).foreach
        }
      }
    }

    "update an existing User" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
          _ <- usersReads.userReads.requireById(moderatorID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => userCreate)
          toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
          ids = toCreate.map(_._1.id)
          created <- ids.traverse(usersReads.userReads.requireById).eventually()
          updateData = created.zipWithIndex.collect {
            case (User(id, data), 0) =>
              User.Update(
                id                = id,
                moderatorID       = moderatorID.some,
                newUsername       = Updatable.Set(data.username),
                newDescription    = OptionUpdatable.setFromOption(data.description),
                newPassword       = Updatable.Set(data.password),
                updatePermissions = List(Permission.Update.Add(Permission.EditProfile(id)))
              )
            case (User(id, _), 1) =>
              User.Update(
                id                = id,
                moderatorID       = moderatorID.some,
                newUsername       = Updatable.Keep,
                newDescription    = OptionUpdatable.Keep,
                newPassword       = Updatable.Keep,
                updatePermissions = List.empty
              )
            case (User(id, _), 2) =>
              User.Update(
                id                = id,
                moderatorID       = moderatorID.some,
                newUsername       = Updatable.Keep,
                newDescription    = OptionUpdatable.Erase,
                newPassword       = Updatable.Keep,
                updatePermissions = List(Permission.Update.Add(Permission.ModerateUsers))
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
              case ((User(id, older), User(_, newer)), 0) =>
                // set case
                older.lens(_.permissions).set(Permissions.empty.append(Permission.EditProfile(id))) must_=== newer
                  .lens(_.lastModifiedAt)
                  .set(None)
              case ((User(_, older), User(_, newer)), 1) =>
                // keep case
                older must_=== newer
              case ((User(_, older), User(_, newer)), 2) =>
                // erase case
                older
                  .lens(_.permissions)
                  .set(Permissions.empty.append(Permission.ModerateUsers))
                  .lens(_.description)
                  .set(None) must_=== newer.lens(_.lastModifiedAt).set(None)
            }
            .lastOption
            .getOrElse(true must beFalse)
        }
      }
    }

    "allow delete of a created User" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.id)
          _ <- usersReads.userReads.requireById(moderatorID).eventually()
          creationData <- (0 until 3).toList.traverse(_ => userCreate)
          // when
          toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
          ids = toCreate.map(_._1.id)
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
          notExist must contain(beFalse).foreach
          areDeleted must contain(beTrue).foreach
        }
      }
    }

    "allow password checking" in {
      usersWrites.runProjector.use { usersProjector =>
        for {
          // given
          _ <- usersProjector.logError("Error reported by Users projector").start
          goodPassword <- passwordCreate("password")
          userId <- userCreate
            .map(_.copy(password = goodPassword))
            .flatMap(usersWrites.userWrites.createUser)
            .map(_._1.id)
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
