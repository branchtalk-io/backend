package io.branchtalk.discussions.api

import cats.effect.{ ContextShift, Sync }
import io.branchtalk.api.Authentication
import io.branchtalk.discussions.api.PostModels.PostError
import io.branchtalk.discussions.model.{ Post, User }
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.shared.models.ID
import io.branchtalk.shared.models.UUIDGenerator.FastUUIDGenerator
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

import scala.annotation.nowarn

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](writes: PostWrites[F]) {

  // TODO: create some AuthService which would take Authentication and list of accesses to validate
  @nowarn("cat=unused")
  private def mockAuth(auth: Authentication): F[ID[User]] = FastUUIDGenerator.create[F].map(ID[User])

  // TODO: handle errors in some nice way?

  // TODO: newest and read

  private val create = PostAPIs.create.toRoutes {
    case (auth, createData) =>
      for {
        userID <- mockAuth(auth)
        data = createData.into[Post.Create].withFieldConst(_.authorID, userID).transform
        result <- writes.createPost(data)
      } yield PostModels.CreatePostResponse(result.id).asRight[PostError]
  }

  private val update = PostAPIs.update.toRoutes {
    case (auth, postID, updateData) =>
      for {
        userID <- mockAuth(auth)
        data = updateData
          .into[Post.Update]
          .withFieldConst(_.id, postID)
          .withFieldConst(_.editorID, userID)
          .withFieldRenamed(_.content, _.newContent)
          .withFieldRenamed(_.title, _.newTitle)
          .transform
        result <- writes.updatePost(data)
      } yield PostModels.UpdatePostResponse(result.id).asRight[PostError]
  }

  private val delete = PostAPIs.delete.toRoutes {
    case (auth, postID) =>
      for {
        userID <- mockAuth(auth)
        data = Post.Delete(postID, userID)
        result <- writes.deletePost(data)
      } yield PostModels.DeletePostResponse(result.id).asRight[PostError]
  }

  val postRoutes: HttpRoutes[F] = create <+> update <+> delete
}
