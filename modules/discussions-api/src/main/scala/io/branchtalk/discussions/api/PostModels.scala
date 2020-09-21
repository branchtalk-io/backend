package io.branchtalk.discussions.api

import java.net.URI

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.branchtalk.ADT
import io.branchtalk.api._
import io.branchtalk.discussions.model._
import io.branchtalk.shared.models.{ ID, Updatable }
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
object PostModels {

  // properties codecs
  implicit val postUrlTitleCodec: JsCodec[Post.UrlTitle] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Post.UrlTitle]
  implicit val postTitleCodec: JsCodec[Post.Title] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Post.Title]
  implicit val postURLCodec: JsCodec[Post.URL] =
    summonCodec[String](JsonCodecMaker.make)
      .mapDecode[URI](s => Try(URI.create(s)).fold(_ => Left(s"Invalid URI: $s"), Right(_)))(_.toString)
      .asNewtype[Post.URL]
  implicit val postTextCodec: JsCodec[Post.Text] =
    summonCodec[String](JsonCodecMaker.make).asNewtype[Post.Text]

  // properties schemas
  implicit val postUrlTitleSchema: Schema[Post.UrlTitle] =
    summonSchema[String Refined NonEmpty].asNewtype[Post.UrlTitle]
  implicit val postTitleSchema: Schema[Post.Title] =
    summonSchema[String Refined NonEmpty].asNewtype[Post.Title]
  implicit val postURLSchema: Schema[Post.URL] =
    summonSchema[String].asNewtype[Post.URL]
  implicit val postTextSchema: Schema[Post.Text] =
    summonSchema[String].asNewtype[Post.Text]

  sealed trait PostError extends ADT
  object PostError {

    final case class NotFound(msg:           String) extends PostError
    final case class ValidationFailed(error: NonEmptyList[String]) extends PostError

    implicit val codec: JsCodec[PostError] = JsonCodecMaker.make[PostError]
  }

  // TODO: consider adding timestamps
  final case class APIPost(
    channelID: ID[Channel],
    urlTitle:  Post.UrlTitle,
    title:     Post.Title,
    content:   Post.Content
  )
  object APIPost {
    implicit val codec: JsCodec[APIPost] = JsonCodecMaker.make[APIPost]
  }

  final case class CreatePostRequest(
    channelID: ID[Channel],
    urlTitle:  Post.UrlTitle,
    title:     Post.Title,
    content:   Post.Content
  )
  object CreatePostRequest {
    implicit val codec: JsCodec[CreatePostRequest] = JsonCodecMaker.make[CreatePostRequest]
  }

  final case class CreatePostResponse(id: ID[Post])
  object CreatePostResponse {
    implicit val codec: JsCodec[CreatePostResponse] = JsonCodecMaker.make[CreatePostResponse]
  }

  final case class UpdatePostRequest(
    title:   Updatable[Post.Title],
    content: Updatable[Post.Content]
  )
  object UpdatePostRequest {
    @nowarn("cat=unused") // macros
    implicit val codec: JsCodec[UpdatePostRequest] = JsonCodecMaker.make[UpdatePostRequest]
  }

  final case class UpdatePostResponse(id: ID[Post])
  object UpdatePostResponse {
    implicit val codec: JsCodec[UpdatePostResponse] = JsonCodecMaker.make[UpdatePostResponse]
  }

  final case class DeletePostResponse(id: ID[Post])
  object DeletePostResponse {
    implicit val codec: JsCodec[DeletePostResponse] = JsonCodecMaker.make[DeletePostResponse]
  }
}
