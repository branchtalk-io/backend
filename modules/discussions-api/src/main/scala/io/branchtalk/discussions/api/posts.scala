package io.branchtalk.discussions.api

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.ADT
import io.branchtalk.shared.models.UUID
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
object posts { // scalastyle:ignore

  // TODO: move to some common
  final case class SessionID(id: UUID)
  object SessionID {
    implicit val codec: Codec[List[String], SessionID, CodecFormat.TextPlain] =
      implicitly[Codec[List[String], UUID, CodecFormat.TextPlain]].map[SessionID](SessionID(_))(_.id)
  }

  // TODO: create PostId or sth here

  sealed trait PostErrors extends ADT
  object PostErrors {
    final case class SomeError(msg: String) extends PostErrors

    implicit val codec: JsonValueCodec[PostErrors] = JsonCodecMaker.make[PostErrors]
  }

  final case class CreatePostRequest(id: UUID)
  object CreatePostRequest {
    implicit val codec: JsonValueCodec[CreatePostRequest] = JsonCodecMaker.make[CreatePostRequest]
  }
  final case class CreatePostResponse()
  object CreatePostResponse {
    @nowarn("cat=unused")
    implicit val codec: JsonValueCodec[CreatePostResponse] = JsonCodecMaker.make[CreatePostResponse]
  }

  val create: Endpoint[(SessionID, UUID, CreatePostRequest), PostErrors, CreatePostResponse, Nothing] =
    endpoint.post
      .in(auth.bearer[SessionID])
      .in("discussions" / "post" / path[UUID])
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[CreatePostResponse])
      .errorOut(jsonBody[PostErrors])

  final case class UpdatePostRequest(id: UUID)
  object UpdatePostRequest {
    implicit val codec: JsonValueCodec[UpdatePostRequest] = JsonCodecMaker.make[UpdatePostRequest]
  }
  final case class UpdatePostResponse()
  object UpdatePostResponse {
    @nowarn("cat=unused")
    implicit val codec: JsonValueCodec[UpdatePostResponse] = JsonCodecMaker.make[UpdatePostResponse]
  }

  val update: Endpoint[(SessionID, UUID, UpdatePostRequest), PostErrors, UpdatePostResponse, Nothing] =
    endpoint.put
      .in(auth.bearer[SessionID])
      .in("discussions" / "post" / path[UUID])
      .in(jsonBody[UpdatePostRequest])
      .out(jsonBody[UpdatePostResponse])
      .errorOut(jsonBody[PostErrors])

  final case class DeletePostResponse(id: UUID)
  object DeletePostResponse {
    implicit val codec: JsonValueCodec[DeletePostResponse] = JsonCodecMaker.make[DeletePostResponse]
  }

  val delete: Endpoint[(SessionID, UUID), PostErrors, DeletePostResponse, Nothing] =
    endpoint.put
      .in(auth.bearer[SessionID])
      .in("discussions" / "post" / path[UUID])
      .out(jsonBody[DeletePostResponse])
      .errorOut(jsonBody[PostErrors])
}
