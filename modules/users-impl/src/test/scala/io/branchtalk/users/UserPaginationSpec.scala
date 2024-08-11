package io.branchtalk.users

import io.branchtalk.shared.model.*
import io.branchtalk.users.model.Permission.ModerateChannel
import io.branchtalk.users.model.{ Permission, Permissions, User }
import org.specs2.mutable.Specification

import scala.concurrent.duration.DurationInt

final class UserPaginationSpec extends Specification, UsersIOTest, UsersFixtures {

  // User pagination tests cannot be run in parallel to other User tests (no parent to filter other tests)
  sequential

  protected given uuidGenerator: TestUUIDGenerator = new TestUUIDGenerator

  "User pagination" should {

    "paginate newest Users" in {
      for {
        // given
        paginatedData <- (0 until 10).toList.traverse(_ => userCreate)
        paginatedIDs <- paginatedData.traverse(usersWrites.userWrites.createUser).map(_.map(_._1.unwrap))
        _ <- paginatedIDs
          .traverse(usersReads.userReads.requireById(_))
          .eventually(delay = 1.second, timeout = 30.seconds)
        // when
        pagination <- usersReads.userReads.paginate(User.Sorting.Newest, Paginated.Offset(0L), Paginated.Limit(5))
        pagination2 <- usersReads.userReads.paginate(User.Sorting.Newest, Paginated.Offset(5L), Paginated.Limit(5))
      } yield {
        // then
        pagination.entities must haveSize(5)
        pagination.nextOffset.map(_.unwrap) must beSome(5L)
        pagination2.entities must haveSize(5)
      }
    }

    "paginate Users by name alphabetically" in {
      for {
        // given
        paginatedData <- (0 until 10).toList.traverse(_ => userCreate)
        paginatedIDs <- paginatedData.traverse(usersWrites.userWrites.createUser).map(_.map(_._1.unwrap))
        _ <- paginatedIDs
          .traverse(usersReads.userReads.requireById(_))
          .eventually(delay = 1.second, timeout = 30.seconds)
        // when
        pagination <- usersReads.userReads.paginate(User.Sorting.NameAlphabetically,
                                                    Paginated.Offset(0L),
                                                    Paginated.Limit(5)
        )
        pagination2 <- usersReads.userReads.paginate(User.Sorting.NameAlphabetically,
                                                     Paginated.Offset(5L),
                                                     Paginated.Limit(5)
        )
      } yield {
        // then
        pagination.entities must haveSize(5)
        pagination.nextOffset.map(_.unwrap) must beSome(5L)
        pagination2.entities must haveSize(5)
      }
    }

    "paginate Users by email alphabetically" in {
      for {
        // given
        paginatedData <- (0 until 10).toList.traverse(_ => userCreate)
        paginatedIDs <- paginatedData.traverse(usersWrites.userWrites.createUser).map(_.map(_._1.unwrap))
        _ <- paginatedIDs
          .traverse(usersReads.userReads.requireById(_))
          .eventually(delay = 1.second, timeout = 30.seconds)
        // when
        pagination <- usersReads.userReads.paginate(User.Sorting.EmailAlphabetically,
                                                    Paginated.Offset(0L),
                                                    Paginated.Limit(5)
        )
        pagination2 <- usersReads.userReads.paginate(User.Sorting.EmailAlphabetically,
                                                     Paginated.Offset(5L),
                                                     Paginated.Limit(5)
        )
      } yield {
        // then
        pagination.entities must haveSize(5)
        pagination.nextOffset.map(_.unwrap) must beSome(5L)
        pagination2.entities must haveSize(5)
      }
    }

    "paginate Users filtered by permissions" in {
      for {
        // given
        channelID <- channelIDCreate
        permissions = List(
          Permission.Administrate,
          Permission.ModerateUsers,
          ModerateChannel(channelID)
        )
        creationdData <- permissions.traverse(_ => userCreate)
        paginatedIDs <- creationdData.traverse(usersWrites.userWrites.createUser).map(_.map(_._1.unwrap))
        _ <- paginatedIDs
          .traverse(usersReads.userReads.requireById(_))
          .eventually(delay = 1.second, timeout = 30.seconds)
        updateData = (paginatedIDs zip permissions).map { case (userID, permission) =>
          User.Update(
            id = userID,
            moderatorID = None,
            newUsername = Updatable.Keep,
            newDescription = OptionUpdatable.Keep,
            newPassword = Updatable.Keep,
            updatePermissions = List(Permission.Update.Add(permission))
          )
        }
        _ <- updateData.traverse(usersWrites.userWrites.updateUser)
        _ <- paginatedIDs
          .traverse(usersReads.userReads.requireById)
          .assert("Users should be eventually updated")(_.forall(_.data.lastModifiedAt.isDefined))
          .eventually(delay = 1.second, timeout = 30.seconds)
        // when
        paginations1 <- permissions.traverse(permission =>
          usersReads.userReads.paginate(User.Sorting.Newest,
                                        Paginated.Offset(0L),
                                        Paginated.Limit(1),
                                        List(User.Filter.HasPermission(permission))
          )
        )
        paginations2 <- permissions.traverse(permission =>
          usersReads.userReads.paginate(
            User.Sorting.Newest,
            Paginated.Offset(0L),
            Paginated.Limit(1),
            List(User.Filter.HasPermissions(Permissions(Set(permission))))
          )
        )
      } yield {
        // then
        paginations1.map(_.entities.size) === permissions.map(_ => 1)
        paginations2.map(_.entities.size) === permissions.map(_ => 1)
      }
    }
  }
}
