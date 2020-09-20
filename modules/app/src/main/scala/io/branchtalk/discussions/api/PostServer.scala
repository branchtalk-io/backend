package io.branchtalk.discussions.api

import cats.effect.{ ContextShift, Sync }
import io.branchtalk.discussions.api.models.PostErrors
import io.branchtalk.discussions.model.Post
import io.branchtalk.discussions.writes.PostWrites
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](writes: PostWrites[F]) {

  // TODO: create some AuthService which would take Authentication and list of accesses to validate

  private val create = posts.create.toRoutes {
    case (_, _, _) =>
      writes.createPost(??? : Post.Create).map(_.transformInto[models.CreatePostResponse]).map(_.asRight[PostErrors])
  }

  private val update = posts.update.toRoutes {
    case (_, _, _) =>
      writes.updatePost(??? : Post.Update).map(_.transformInto[models.UpdatePostResponse]).map(_.asRight[PostErrors])
  }

  private val delete = posts.delete.toRoutes {
    case (_, _) =>
      writes.deletePost(??? : Post.Delete).map(_ => ??? : models.DeletePostResponse).map(_.asRight[PostErrors])
  }

  val postRoutes: HttpRoutes[F] = create <+> update <+> delete
}
