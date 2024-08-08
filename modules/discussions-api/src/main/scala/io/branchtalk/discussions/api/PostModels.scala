package io.branchtalk.discussions.api

import java.net.URI

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.discussions.model.*
import io.branchtalk.shared.model.*
import io.scalaland.chimney.dsl.*
import sttp.tapir.Schema
import sttp.tapir.generic.Configuration

import scala.annotation.unused
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object PostModels {

  // properties codecs
  given JsCodec[Post.UrlTitle] = newtypeCodec
  given JsCodec[Post.Title]    = newtypeCodec
  given JsCodec[Post.URL] =
    DefaultJsCodec
      .derived[String]
      .mapDecode[URI](s => Try(URI.create(s)).fold(_ => Left(s"Invalid URI: $s"), Right(_)))(_.toString)
      .asNewtypeCodec[Post.URL]
  given JsCodec[Post.Text] = newtypeCodec
  given JsCodec[Post.Content] = {
    inline given CodecMakerConfig = CodecMakerConfig.withAdtLeafClassNameMapper(adtDiscriminatorNameMapper)
    DefaultJsCodec.derived[Post.Content]
  }
  given JsCodec[Post.CommentsNr] = newtypeCodec

  // properties schemas
  given JsSchema[Post.Content] = {
    // used in macros
    @unused given Configuration = Configuration.default.copy(toEncodedName = adtDiscriminatorNameMapper)
    Schema.derived[Post.Content]
  }

  sealed trait PostError derives DefaultJsCodec, JsSchema
  object PostError {

    final case class BadCredentials(msg: String) extends PostError derives DefaultJsCodec, JsSchema
    final case class NoPermission(msg: String) extends PostError derives DefaultJsCodec, JsSchema
    final case class NotFound(msg: String) extends PostError derives DefaultJsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends PostError derives DefaultJsCodec, JsSchema
  }

  final case class APIPost(
    id:         ID[Post],
    channelID:  ID[Channel],
    urlTitle:   Post.UrlTitle,
    title:      Post.Title,
    content:    Post.Content,
    commentsNr: Post.CommentsNr
  ) derives DefaultJsCodec,
        JsSchema
  object APIPost {

    def fromDomain(post: Post): APIPost = post.data.into[APIPost].withFieldConst(_.id, post.id).transform
  }

  final case class CreatePostRequest(
    title:   Post.Title,
    content: Post.Content
  ) derives DefaultJsCodec,
        JsSchema

  final case class CreatePostResponse(id: ID[Post]) derives DefaultJsCodec, JsSchema

  final case class UpdatePostRequest(
    newTitle:   Updatable[Post.Title],
    newContent: Updatable[Post.Content]
  ) derives DefaultJsCodec,
        JsSchema

  final case class UpdatePostResponse(id: ID[Post]) derives DefaultJsCodec, JsSchema

  final case class DeletePostResponse(id: ID[Post]) derives DefaultJsCodec, JsSchema

  final case class RestorePostResponse(id: ID[Post]) derives DefaultJsCodec, JsSchema
}
