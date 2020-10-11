package io.branchtalk.users.writes

import cats.data.NonEmptyList
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import fs2.Stream
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.models.UUID
import io.branchtalk.users.events.{ UserCommandEvent, UserEvent, UsersCommandEvent, UsersEvent }
import io.branchtalk.users.infrastructure.DoobieExtensions._
import io.branchtalk.users.model.{ Permissions, Session }
import io.scalaland.chimney.dsl._

final class UserProjector[F[_]: Sync](transactor: Transactor[F])
    extends Projector[F, UsersCommandEvent, (UUID, UsersEvent)] {

  private val logger = Logger(getClass)

  private implicit val logHandler: LogHandler = doobieLogger(getClass)

  override def apply(in: Stream[F, UsersCommandEvent]): Stream[F, (UUID, UsersEvent)] =
    in.collect {
        case UsersCommandEvent.ForUser(event) => event
      }
      .evalMap[F, (UUID, UserEvent)] {
        case event: UserCommandEvent.Create => toCreate(event).widen
        case event: UserCommandEvent.Update => toUpdate(event).widen
        case event: UserCommandEvent.Delete => toDelete(event).widen
      }
      .map {
        case (key, value) => key -> UsersEvent.ForUser(value)
      }
      .handleErrorWith { error =>
        logger.error("User event processing failed", error)
        Stream.empty
      }

  def toCreate(event: UserCommandEvent.Create): F[(UUID, UserEvent.Created)] = {
    val Session.Usage.Tupled(sessionType, sessionPermissions) = Session.Usage.UserSession
    sql"""INSERT INTO users (
         |  id,
         |  email,
         |  username,
         |  description,
         |  passwd_algorithm,
         |  passwd_hash,
         |  passwd_salt,
         |  permissions,
         |  created_at
         |)
         |VALUES (
         |  ${event.id},
         |  ${event.email},
         |  ${event.username},
         |  ${event.description},
         |  ${event.password.algorithm},
         |  ${event.password.hash},
         |  ${event.password.salt},
         |  ${Permissions(Set.empty)},
         |  ${event.createdAt}
         |)
         |ON CONFLICT (id) DO NOTHING""".stripMargin.update.run >>
      sql"""INSERT INTO sessions (
           |  id,
           |  user_id,
           |  usage_type,
           |  permissions,
           |  expires_at
           |)
           |VALUES (
           |  ${event.sessionID},
           |  ${event.id},
           |  ${sessionType},
           |  ${sessionPermissions},
           |  ${event.sessionExpiresAt}
           |)""".stripMargin.update.run
  }.transact(transactor) >>
    (event.id.uuid -> event.transformInto[UserEvent.Created]).pure[F]

  def toUpdate(event: UserCommandEvent.Update): F[(UUID, UserEvent.Updated)] =
    (NonEmptyList.fromList(
      List(
        event.newUsername.toUpdateFragment(fr"username"),
        event.newDescription.toUpdateFragment(fr"description"),
        event.newPassword.fold(
          pass => fr"passwd_algorithm = ${pass.algorithm}, passwd_hash = ${pass.hash}, passwd_salt = ${pass.salt}".some,
          none[Fragment]
        )
      ).flatten
    ) match {
      case Some(updates) =>
        (fr"UPDATE users SET" ++
          (updates :+ fr"last_modified_at = ${event.modifiedAt}").intercalate(fr",") ++
          fr"WHERE id = ${event.id}").update.run.transact(transactor).void
      case None =>
        Sync[F].delay(logger.warn(s"User update ignored as it doesn't contain any modification:\n${event.show}"))
    }) >>
      (event.id.uuid -> event.transformInto[UserEvent.Updated]).pure[F]

  def toDelete(event: UserCommandEvent.Delete): F[(UUID, UserEvent.Deleted)] =
    (sql"DELETE FROM users WHERE id = ${event.id}".update.run >>
      sql"INSERT INTO deleted_users (id, deleted_at) VALUES (${event.id}, ${event.deletedAt}) ON CONFLICT (id) DO NOTHING".update.run)
      .transact(transactor) >>
      (event.id.uuid -> event.transformInto[UserEvent.Deleted]).pure[F]
}
