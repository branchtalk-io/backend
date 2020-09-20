package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.ADT
import io.branchtalk.api._
import io.branchtalk.shared.models.ID

import scala.annotation.nowarn

@SuppressWarnings(
// for macros
  Array(
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.Null",
    "org.wartremover.warts.TraversableOps",
    "org.wartremover.warts.Var",
    "org.wartremover.warts.While"
  )
)
object models { // scalastyle:ignore object.name

  // TODO: all classes basically?

  sealed trait PostErrors extends ADT
  object PostErrors {

    final case class NotFound(msg:           String) extends PostErrors
    final case class ValidationFailed(error: NonEmptyList[String]) extends PostErrors

    implicit val codec: JsonValueCodec[PostErrors] = JsonCodecMaker.make[PostErrors]
  }

  // TODO: ApiPost + codec
  final case class APIPost(
    )
  object APIPost {
    @nowarn("cat=unused")
    implicit val codec: JsonValueCodec[APIPost] = JsonCodecMaker.make[APIPost]
  }

  final case class CreatePostRequest(id: ID[APIPost])
  object CreatePostRequest {
    implicit val codec: JsonValueCodec[CreatePostRequest] = JsonCodecMaker.make[CreatePostRequest]
  }

  final case class CreatePostResponse()
  object CreatePostResponse {
    @nowarn("cat=unused")
    implicit val codec: JsonValueCodec[CreatePostResponse] = JsonCodecMaker.make[CreatePostResponse]
  }

  final case class UpdatePostRequest(id: ID[APIPost])
  object UpdatePostRequest {
    implicit val codec: JsonValueCodec[UpdatePostRequest] = JsonCodecMaker.make[UpdatePostRequest]
  }

  final case class UpdatePostResponse()
  object UpdatePostResponse {
    @nowarn("cat=unused")
    implicit val codec: JsonValueCodec[UpdatePostResponse] = JsonCodecMaker.make[UpdatePostResponse]
  }

  final case class DeletePostResponse(id: ID[APIPost])
  object DeletePostResponse {
    implicit val codec: JsonValueCodec[DeletePostResponse] = JsonCodecMaker.make[DeletePostResponse]
  }
}
