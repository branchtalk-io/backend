package io.branchtalk.discussions.api

import cats.effect.{ ContextShift, Sync }
import cats.implicits._
import io.branchtalk.discussions.api.posts.PostErrors
import io.branchtalk.discussions.models.Post
import io.branchtalk.discussions.writes.PostWrites
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](writes: PostWrites[F]) {

  val postRoutes: HttpRoutes[F] = posts.create.toRoutes { _: posts.CreatePostRequest =>
    // TODO: input.transformInto[Post.Create]
    writes.createPost(??? : Post.Create).map(_.transformInto[posts.CreatePostResponse]).map(_.asRight[PostErrors])
  }
}
