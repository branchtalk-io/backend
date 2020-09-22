package io.branchtalk.discussions.api

import cats.data.NonEmptySet
import cats.effect.{ ContextShift, Sync }
import io.branchtalk.api.{ Authentication, Pagination }
import io.branchtalk.discussions.api.PostModels.{ APIPost, PostError }
import io.branchtalk.discussions.model.{ Channel, Post, User }
import io.branchtalk.discussions.reads.PostReads
import io.branchtalk.discussions.writes.PostWrites
import io.branchtalk.shared.models.ID
import io.branchtalk.shared.models.UUIDGenerator.FastUUIDGenerator
import io.scalaland.chimney.dsl._
import org.http4s._
import sttp.tapir.server.http4s._

import scala.annotation.nowarn

final class PostServer[F[_]: Http4sServerOptions: Sync: ContextShift](reads: PostReads[F], writes: PostWrites[F]) {

  // TODO: log/measure all access attempts?

  // TODO: create some AuthService which would take Authentication and list of accesses to validate
  @nowarn("cat=unused")
  private def mockAuth(auth: Authentication): F[ID[User]] = FastUUIDGenerator.create[F].map(ID[User])

  // TODO: handle errors in some nice way?

  // TODO: newest and read

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null")) // temporarily
  private val newest = PostAPIs.newest.toRoutes {
    case (optAuth, optOffset, optLimit) =>
      // TODO: validate limit using some config
      // TODO: default offset and limit
      val offset = optOffset.getOrElse(???)
      val limit  = optLimit.getOrElse(???)
      for {
        _ <- optAuth.traverse(mockAuth)
        channelIDs = null.asInstanceOf[NonEmptySet[ID[Channel]]] // scalastyle:ignore
        paginated <- reads.paginate(channelIDs, offset.value, limit.value)
      } yield Pagination.fromPaginated(paginated.map(APIPost.fromDomain), offset, limit).asRight[PostError]
  }

  private val create = PostAPIs.create.toRoutes {
    case (auth, createData) =>
      for {
        userID <- mockAuth(auth)
        data = createData.into[Post.Create].withFieldConst(_.authorID, userID).transform
        result <- writes.createPost(data)
      } yield PostModels.CreatePostResponse(result.id).asRight[PostError]
  }

  private val read = PostAPIs.read.toRoutes {
    case (optAuth, postID) =>
      for {
        _ <- optAuth.traverse(mockAuth)
        result <- reads.requireById(postID)
      } yield APIPost.fromDomain(result).asRight[PostError]
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

  val postRoutes: HttpRoutes[F] = newest <+> create <+> read <+> update <+> delete
}
