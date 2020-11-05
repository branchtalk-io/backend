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
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._
import sttp.tapir.Schema

import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
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
    summonSchema[URI].asNewtype[Post.URL]
  implicit val postTextSchema: Schema[Post.Text] =
    summonSchema[String].asNewtype[Post.Text]

  @Semi(JsCodec) sealed trait PostError extends ADT
  object PostError {

    @Semi(JsCodec) final case class BadCredentials(msg: String) extends PostError
    @Semi(JsCodec) final case class NoPermission(msg: String) extends PostError
    @Semi(JsCodec) final case class NotFound(msg: String) extends PostError
    @Semi(JsCodec) final case class ValidationFailed(error: NonEmptyList[String]) extends PostError
  }

  @Semi(JsCodec) final case class APIPost(
    id:        ID[Post],
    channelID: ID[Channel],
    urlTitle:  Post.UrlTitle,
    title:     Post.Title,
    content:   Post.Content
  )
  object APIPost {

    def fromDomain(post: Post): APIPost = post.data.into[APIPost].withFieldConst(_.id, post.id).transform
  }

  @Semi(JsCodec) final case class CreatePostRequest(
    channelID: ID[Channel],
    title:     Post.Title,
    content:   Post.Content
  )

  @Semi(JsCodec) final case class CreatePostResponse(id: ID[Post])

  @Semi(JsCodec) final case class UpdatePostRequest(
    title:   Updatable[Post.Title],
    content: Updatable[Post.Content]
  )

  @Semi(JsCodec) final case class UpdatePostResponse(id: ID[Post])

  @Semi(JsCodec) final case class DeletePostResponse(id: ID[Post])
}
