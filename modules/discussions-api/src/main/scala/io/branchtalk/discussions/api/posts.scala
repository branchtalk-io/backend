package io.branchtalk.discussions.api

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.ADT
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

  val create: Endpoint[CreatePostRequest, PostErrors, CreatePostResponse, Nothing] =
    endpoint
      .in("discussions" / "post")
      .in(jsonBody[CreatePostRequest])
      .out(jsonBody[CreatePostResponse])
      .errorOut(jsonBody[PostErrors])
}
