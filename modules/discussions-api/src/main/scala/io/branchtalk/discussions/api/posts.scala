package io.branchtalk.discussions.api

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.ADT
import io.branchtalk.api._
import io.branchtalk.api.AuthenticationSupport._
import io.branchtalk.shared.models.ID
import sttp.tapir._
import sttp.tapir.json.jsoniter._

import scala.annotation.nowarn

@SuppressWarnings(
  // for macros
  Array("org.wartremover.warts.Equals",
        "org.wartremover.warts.Null",
        "org.wartremover.warts.Var",
        "org.wartremover.warts.While")
)
object posts { // scalastyle:ignore object.name

  sealed trait PostErrors extends ADT
  object PostErrors {
    final case class SomeError(msg: String) extends PostErrors

    implicit val codec: JsonValueCodec[PostErrors] = JsonCodecMaker.make[PostErrors]
  }

  // TODO: new pagination
  // TODO: hot pagination
  // TODO: controversial pagination

  final case class CreatePostRequest(id: ID[APIPost])
  object CreatePostRequest {
    implicit val codec: JsonValueCodec[CreatePostRequest] = JsonCodecMaker.make[CreatePostRequest]
  }
  final case class CreatePostResponse()
  object CreatePostResponse {
    @nowarn("cat=unused")
    implicit val codec: JsonValueCodec[CreatePostResponse] = JsonCodecMaker.make[CreatePostResponse]
  }

  val create: Endpoint[(Authentication, ID[APIPost], CreatePostRequest), PostErrors, CreatePostResponse, Nothing] =
    endpoint.post
      .in(authHeader)
      .in("discussions" / "posts" / path[ID[APIPost]])
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[CreatePostResponse])
      .errorOut(jsonBody[PostErrors])

  // TODO: show

  final case class UpdatePostRequest(id: ID[APIPost])
  object UpdatePostRequest {
    implicit val codec: JsonValueCodec[UpdatePostRequest] = JsonCodecMaker.make[UpdatePostRequest]
  }
  final case class UpdatePostResponse()
  object UpdatePostResponse {
    @nowarn("cat=unused")
    implicit val codec: JsonValueCodec[UpdatePostResponse] = JsonCodecMaker.make[UpdatePostResponse]
  }

  val update: Endpoint[(Authentication, ID[APIPost], UpdatePostRequest), PostErrors, UpdatePostResponse, Nothing] =
    endpoint.put
      .in(authHeader)
      .in("discussions" / "posts" / path[ID[APIPost]])
      .in(jsonBody[UpdatePostRequest])
      .out(jsonBody[UpdatePostResponse])
      .errorOut(jsonBody[PostErrors])

  final case class DeletePostResponse(id: ID[APIPost])
  object DeletePostResponse {
    implicit val codec: JsonValueCodec[DeletePostResponse] = JsonCodecMaker.make[DeletePostResponse]
  }

  val delete: Endpoint[(Authentication, ID[APIPost]), PostErrors, DeletePostResponse, Nothing] =
    endpoint.put
      .in(authHeader)
      .in("discussions" / "posts" / path[ID[APIPost]])
      .out(jsonBody[DeletePostResponse])
      .errorOut(jsonBody[PostErrors])
}
