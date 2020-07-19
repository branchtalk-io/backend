package io.branchtalk.discussions.api

import cats.effect.{ ContextShift, Sync }
import cats.implicits._
import io.branchtalk.discussions.api.posts.PostErrors
import io.branchtalk.discussions.models.Post
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.shared.models.UUID
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](writes: PostWrites[F]) {

  private val create = posts.create.toRoutes {
    case (_: UUID, _: posts.CreatePostRequest) =>
      // TODO: input.transformInto[Post.Create]
      writes.createPost(??? : Post.Create).map(_.transformInto[posts.CreatePostResponse]).map(_.asRight[PostErrors])
  }

  private val update = posts.update.toRoutes {
    case (_: UUID, _: posts.UpdatePostRequest) =>
      // TODO: input.transformInto[Post.Update]
      writes.updatePost(??? : Post.Update).map(_.transformInto[posts.UpdatePostResponse]).map(_.asRight[PostErrors])
  }

  private val delete = posts.delete.toRoutes {
    case (_: UUID) =>
      // TODO: input.transformInto[Post.Delete]
      writes.deletePost(??? : Post.Delete).map(_.transformInto[posts.DeletePostResponse]).map(_.asRight[PostErrors])
  }

  val postRoutes: HttpRoutes[F] = create <+> update <+> delete
}
