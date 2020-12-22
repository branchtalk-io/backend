package io.branchtalk.users.writes

import cats.effect.Sync
import fs2.Stream
import io.branchtalk.discussions.events.{ ChannelEvent, DiscussionEvent }
import io.branchtalk.shared.infrastructure.Projector
import io.branchtalk.shared.model.{ ID, ModificationTime, OptionUpdatable, UUID, Updatable }
import io.branchtalk.users.events.{ UserCommandEvent, UsersCommandEvent }
import io.branchtalk.users.model.{ Channel, Permission, User }

final class DiscussionsConsumer[F[_]: Sync] extends Projector[F, DiscussionEvent, (UUID, UsersCommandEvent)] {

  override def apply(in: Stream[F, DiscussionEvent]): Stream[F, (UUID, UsersCommandEvent)] =
    in.collect { case DiscussionEvent.ForChannel(created: ChannelEvent.Created) =>
      created.authorID.uuid -> UsersCommandEvent.ForUser(toGrantedChannelModerator(created))
    }

  def toGrantedChannelModerator(created: ChannelEvent.Created): UserCommandEvent.Update =
    UserCommandEvent.Update(
      id = ID[User](created.authorID.uuid),
      moderatorID = None,
      newUsername = Updatable.Keep,
      newDescription = OptionUpdatable.Keep,
      newPassword = Updatable.Keep,
      updatePermissions = List(Permission.Update.Add(Permission.ModerateChannel(ID[Channel](created.id.uuid)))),
      modifiedAt = ModificationTime(created.createdAt.offsetDateTime)
    )
}
