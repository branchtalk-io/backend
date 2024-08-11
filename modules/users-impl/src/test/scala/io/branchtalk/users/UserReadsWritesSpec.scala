package io.branchtalk.users

import cats.effect.IO
import io.branchtalk.shared.model.*
import io.branchtalk.users.model.{ Password, Permission, Permissions, User }
import org.specs2.mutable.Specification

final class UserReadsWritesSpec extends Specification, UsersIOTest, UsersFixtures {

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "User Reads & Writes" should {

    // pagination moved to a separate test

    "create a User and eventually read it" in {
      for {
        // given
        creationData <- (0 until 3).toList.traverse(_ => userCreate)
        // when
        toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
        ids = toCreate.map(_._1.unwrap)
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

    "don't update a User that doesn't exists" in {
      for {
        // given
        moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.unwrap)
        _ <- usersReads.userReads.requireById(moderatorID).eventually()
        creationData <- (0 until 3).toList.traverse(_ => userCreate)
        fakeUpdateData <- creationData.traverse { data =>
          ID.create[IO, User].map { id =>
            User.Update(
              id = id,
              moderatorID = moderatorID.some,
              newUsername = Updatable.Set(data.username),
              newDescription = OptionUpdatable.setFromOption(data.description),
              newPassword = Updatable.Set(data.password),
              updatePermissions = List.empty
            )
          }
        }
        // when
        toUpdate <- fakeUpdateData.traverse(usersWrites.userWrites.updateUser(_).attempt)
      } yield
      // then
      toUpdate must contain(beLeft[Throwable]).foreach
    }

    "update an existing User" in {
      for {
        // given
        moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.unwrap)
        _ <- usersReads.userReads.requireById(moderatorID).eventually()
        creationData <- (0 until 3).toList.traverse(_ => userCreate)
        toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
        ids = toCreate.map(_._1.unwrap)
        created <- ids.traverse(usersReads.userReads.requireById).eventually()
        updateData = created.zipWithIndex.collect {
          case (User(id, data), 0) =>
            User.Update(
              id = id,
              moderatorID = moderatorID.some,
              newUsername = Updatable.Set(data.username),
              newDescription = OptionUpdatable.setFromOption(data.description),
              newPassword = Updatable.Set(data.password),
              updatePermissions = List(Permission.Update.Add(Permission.IsUser(id)))
            )
          case (User(id, _), 1) =>
            User.Update(
              id = id,
              moderatorID = moderatorID.some,
              newUsername = Updatable.Keep,
              newDescription = OptionUpdatable.Keep,
              newPassword = Updatable.Keep,
              updatePermissions = List.empty
            )
          case (User(id, _), 2) =>
            User.Update(
              id = id,
              moderatorID = moderatorID.some,
              newUsername = Updatable.Keep,
              newDescription = OptionUpdatable.Erase,
              newPassword = Updatable.Keep,
              updatePermissions = List(Permission.Update.Add(Permission.ModerateUsers))
            )
        }
        // when
        _ <- updateData.traverse(usersWrites.userWrites.updateUser)
        updated <- ids
          .traverse(usersReads.userReads.requireById)
          .assert("Updated entity should have lastModifiedAt set")(_.last.data.lastModifiedAt.isDefined)
          .eventually()
      } yield
      // then
      created
        .zip(updated)
        .zipWithIndex
        .collect {
          case ((User(id, older), User(_, newer)), 0) =>
            // set case
            older.copy(permissions = Permissions.empty.append(Permission.IsUser(id))) === newer
              .copy(lastModifiedAt = None)
          case ((User(_, older), User(_, newer)), 1) =>
            // keep case
            older === newer
          case ((User(_, older), User(_, newer)), 2) =>
            // erase case
            older.copy(permissions = Permissions.empty.append(Permission.ModerateUsers), description = None) === newer
              .copy(lastModifiedAt = None)
        }
        .lastOption
        .getOrElse(true must beFalse)
    }

    "allow delete of a created User" in {
      for {
        // given
        moderatorID <- userCreate.flatMap(usersWrites.userWrites.createUser).map(_._1.unwrap)
        _ <- usersReads.userReads.requireById(moderatorID).eventually()
        creationData <- (0 until 3).toList.traverse(_ => userCreate)
        // when
        toCreate <- creationData.traverse(usersWrites.userWrites.createUser)
        ids = toCreate.map(_._1.unwrap)
        _ <- ids.traverse(usersReads.userReads.requireById).eventually()
        _ <- ids.map(User.Delete(_, moderatorID.some)).traverse(usersWrites.userWrites.deleteUser)
        _ <- ids
          .traverse(usersReads.userReads.getById)
          .assert("All Users should be eventually deleted")(_.forall(_.isEmpty))
          .eventually()
        notExist <- ids.traverse(usersReads.userReads.exists)
        areDeleted <- ids.traverse(usersReads.userReads.deleted)
      } yield {
        // then
        notExist must contain(beFalse).foreach
        areDeleted must contain(beTrue).foreach
      }
    }

    "allow password checking" in {
      for {
        // given
        goodPassword <- passwordCreate("password")
        rawGoodPassword <- ParseNewtype[IO].parse[Password.Raw]("password".getBytes)
        rawBadPassword <- ParseNewtype[IO].parse[Password.Raw]("bad".getBytes)
        userId <- userCreate
          .map(_.copy(password = goodPassword))
          .flatMap(usersWrites.userWrites.createUser)
          .map(_._1.unwrap)
        user <- usersReads.userReads.requireById(userId).eventually()
        // when
        ok <- usersReads.userReads.authenticate(user.data.username, rawGoodPassword).attempt
        fail <- usersReads.userReads.authenticate(user.data.username, rawBadPassword).attempt
      } yield {
        // then
        ok must beRight(user)
        fail must beLeft(beAnInstanceOf[CommonError.InvalidCredentials])
      }
    }
  }
}
