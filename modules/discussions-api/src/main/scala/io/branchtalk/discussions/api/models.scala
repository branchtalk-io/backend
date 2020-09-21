package io.branchtalk.discussions.api

import java.net.URI

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.branchtalk.ADT
import io.branchtalk.api._
import io.branchtalk.discussions.model._
import io.branchtalk.shared.models.ID
import sttp.tapir.Schema

import scala.annotation.nowarn
import scala.util.Try

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

  // Post properties codecs
  implicit val postUrlTitleCodec: JsonValueCodec[Post.UrlTitle] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Post.UrlTitle]
  implicit val postTitleCodec: JsonValueCodec[Post.Title] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Post.Title]
  implicit val postURLCodec: JsonValueCodec[Post.URL] =
    summonCodec[String](JsonCodecMaker.make)
      .mapDecode[URI](s => Try(URI.create(s)).fold(_ => Left(s"Invalid URI: $s"), Right(_)))(_.toString)
      .asNewtype[Post.URL]
  implicit val postTextCodec: JsonValueCodec[Post.Text] =
    summonCodec[String](JsonCodecMaker.make).asNewtype[Post.Text]

  // Post properties schemas
  implicit val postUrlTitleSchema: Schema[Post.UrlTitle] =
    summonSchema[String Refined NonEmpty].asNewtype[Post.UrlTitle]
  implicit val postTitleSchema: Schema[Post.Title] =
    summonSchema[String Refined NonEmpty].asNewtype[Post.Title]
  implicit val postURLSchema: Schema[Post.URL] =
    summonSchema[String].asNewtype[Post.URL]
  implicit val postTextSchema: Schema[Post.Text] =
    summonSchema[String].asNewtype[Post.Text]

  sealed trait PostErrors extends ADT
  object PostErrors {

    final case class NotFound(msg:           String) extends PostErrors
    final case class ValidationFailed(error: NonEmptyList[String]) extends PostErrors

    implicit val codec: JsonValueCodec[PostErrors] = JsonCodecMaker.make[PostErrors]
  }

  final case class APIPost(
    channelID: ID[Channel],
    urlTitle:  Post.UrlTitle,
    title:     Post.Title,
    content:   Post.Content
  )
  object APIPost {
    implicit val codec: JsonValueCodec[APIPost] = JsonCodecMaker.make[APIPost]
  }

  final case class CreatePostRequest(id: ID[Post])
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
