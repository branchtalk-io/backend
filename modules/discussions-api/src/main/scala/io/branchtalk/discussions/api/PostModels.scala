package io.branchtalk.discussions.api

import java.net.URI

import cats.data.NonEmptyList
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import io.branchtalk.ADT
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.api.TapirSupport.*
import io.branchtalk.discussions.model.*
import io.branchtalk.shared.model.{ ID, Updatable, adtDiscriminatorNameMapper }
import io.scalaland.chimney.dsl.*
import sttp.tapir.Schema
import sttp.tapir.generic.Configuration

import scala.annotation.unused
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object PostModels {

  // properties codecs
  implicit val postUrlTitleCodec: JsCodec[Post.UrlTitle] =
    DefaultJsCodec.derived[String].asNewtypeCodec[Post.UrlTitle]
  implicit val postTitleCodec: JsCodec[Post.Title] =
    DefaultJsCodec.derived[String].asNewtypeCodec[Post.Title]
  implicit val postURLCodec: JsCodec[Post.URL] =
    DefaultJsCodec
      .derived[String]
      .mapDecode[URI](s => Try(URI.create(s)).fold(_ => Left(s"Invalid URI: $s"), Right(_)))(_.toString)
      .asNewtypeCodec[Post.URL]
  implicit val postTextCodec: JsCodec[Post.Text] =
    DefaultJsCodec.derived[String].asNewtypeCodec[Post.Text]
  implicit val postContentCodec: JsCodec[Post.Content] = {
    inline given JsCodecConfig = JsCodecConfig.withAdtLeafClassNameMapper(adtDiscriminatorNameMapper)
    DefaultJsCodec.derived[Post.Content]
  }
  implicit val postRepliesNrCodec: JsCodec[Post.CommentsNr] =
    DefaultJsCodec.derived[Int].asNewtypeCodec[Post.CommentsNr]

  // properties schemas
  implicit val postUrlTitleSchema: JsSchema[Post.UrlTitle] =
    summonSchema[String Refined NonEmpty].asNewtypeSchema[Post.UrlTitle]
  implicit val postTitleSchema: JsSchema[Post.Title] =
    summonSchema[String Refined NonEmpty].asNewtypeSchema[Post.Title]
  implicit val postURLSchema: JsSchema[Post.URL] =
    summonSchema[URI].asNewtypeSchema[Post.URL]
  implicit val postTextSchema: JsSchema[Post.Text] =
    summonSchema[String].asNewtypeSchema[Post.Text]
  implicit val postContentSchema: JsSchema[Post.Content] = {
    // used in macros
    @unused given Configuration = Configuration.default.copy(toEncodedName = adtDiscriminatorNameMapper)
    Schema.derived[Post.Content]
  }
  implicit val postCommentsNrSchema: JsSchema[Post.CommentsNr] =
    summonSchema[Int Refined NonNegative].asNewtypeSchema[Post.CommentsNr]

  sealed trait PostError derives JsCodec, JsSchema
  object PostError {

    final case class BadCredentials(msg: String) extends PostError derives JsCodec, JsSchema
    final case class NoPermission(msg: String) extends PostError derives JsCodec, JsSchema
    final case class NotFound(msg: String) extends PostError derives JsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends PostError derives JsCodec, JsSchema
  }

  final case class APIPost(
    id:         ID[Post],
    channelID:  ID[Channel],
    urlTitle:   Post.UrlTitle,
    title:      Post.Title,
    content:    Post.Content,
    commentsNr: Post.CommentsNr
  ) derives JsCodec,
        JsSchema
  object APIPost {

    def fromDomain(post: Post): APIPost = post.data.into[APIPost].withFieldConst(_.id, post.id).transform
  }

  final case class CreatePostRequest(
    title:   Post.Title,
    content: Post.Content
  ) derives JsCodec,
        JsSchema

  final case class CreatePostResponse(id: ID[Post]) derives JsCodec, JsSchema

  final case class UpdatePostRequest(
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content]
  ) derives JsCodec,
        JsSchema

  final case class UpdatePostResponse(id: ID[Post]) derives JsCodec, JsSchema

  final case class DeletePostResponse(id: ID[Post]) derives JsCodec, JsSchema

  final case class RestorePostResponse(id: ID[Post]) derives JsCodec, JsSchema
}
