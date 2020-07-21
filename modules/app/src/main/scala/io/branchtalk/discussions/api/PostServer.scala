package io.branchtalk.discussions.api

import cats.effect.{ ContextShift, Sync }
import io.branchtalk.discussions.api.posts.{ PostErrors, SessionID }
import io.branchtalk.discussions.models.Post
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.shared.models.UUID
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](writes: PostWrites[F]) {

  // TODO: create some AuthService which would take SessionID and list of accesses to validate if endpoint is available

  private val create = posts.create.toRoutes {
    case (_: SessionID, _: UUID, _: posts.CreatePostRequest) =>
      writes.createPost(??? : Post.Create).map(_.transformInto[posts.CreatePostResponse]).map(_.asRight[PostErrors])
  }

  private val update = posts.update.toRoutes {
    case (_: SessionID, _: UUID, _: posts.UpdatePostRequest) =>
      writes.updatePost(??? : Post.Update).map(_.transformInto[posts.UpdatePostResponse]).map(_.asRight[PostErrors])
  }

  private val delete = posts.delete.toRoutes {
    case (_: SessionID, _: UUID) =>
      writes
        .deletePost(??? : Post.Delete)
        .map(scheduled => posts.DeletePostResponse(scheduled.id.value))
        .map(_.asRight[PostErrors])
  }

  val postRoutes: HttpRoutes[F] = create <+> update <+> delete
}
