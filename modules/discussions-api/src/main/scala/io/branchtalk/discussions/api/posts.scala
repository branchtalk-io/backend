package io.branchtalk.discussions.api

import cats.effect.IO
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.ADT
import io.branchtalk.shared.models.UUID
import sttp.tapir._
import sttp.tapir.json.jsoniter._

@SuppressWarnings(
  // for macros
  Array("org.wartremover.warts.Equals",
        "org.wartremover.warts.Null",
        "org.wartremover.warts.Var",
        "org.wartremover.warts.While")
)
object posts { // scalastyle:ignore

  sealed trait PostErrors extends ADT
  object PostErrors {
    final case class SomeError(msg: String) extends PostErrors

    implicit val codec: JsonValueCodec[PostErrors] = JsonCodecMaker.make[PostErrors]
  }

  final case class CreatePostRequest()
  object CreatePostRequest {
    implicit val codec: JsonValueCodec[CreatePostRequest] = JsonCodecMaker.make[CreatePostRequest]
  }
  final case class CreatePostResponse()
  object CreatePostResponse {
    implicit val codec: JsonValueCodec[CreatePostResponse] = JsonCodecMaker.make[CreatePostResponse]
  }

  val create: Endpoint[(UUID, CreatePostRequest), PostErrors, CreatePostResponse, Nothing] =
    endpoint.post
      .in("discussions" / "post" / path[UUID])
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[CreatePostResponse])
      .errorOut(jsonBody[PostErrors])

  final case class UpdatePostRequest()
  object UpdatePostRequest {
    implicit val codec: JsonValueCodec[UpdatePostRequest] = JsonCodecMaker.make[UpdatePostRequest]
  }
  final case class UpdatePostResponse()
  object UpdatePostResponse {
    implicit val codec: JsonValueCodec[UpdatePostResponse] = JsonCodecMaker.make[UpdatePostResponse]
  }

  val update: Endpoint[(UUID, UpdatePostRequest), PostErrors, UpdatePostResponse, Nothing] =
    endpoint.put
      .in("discussions" / "post" / path[UUID])
      .in(jsonBody[UpdatePostRequest])
      .out(jsonBody[UpdatePostResponse])
      .errorOut(jsonBody[PostErrors])

  final case class DeletePostResponse()
  object DeletePostResponse {
    implicit val codec: JsonValueCodec[DeletePostResponse] = JsonCodecMaker.make[DeletePostResponse]
  }

  val delete: Endpoint[UUID, PostErrors, DeletePostResponse, Nothing] =
    endpoint.put
      .in("discussions" / "post" / path[UUID])
      .out(jsonBody[DeletePostResponse])
      .errorOut(jsonBody[PostErrors])
}
