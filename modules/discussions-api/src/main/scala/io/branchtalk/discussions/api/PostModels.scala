package io.branchtalk.discussions.api

import java.net.URI

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.discussions.model.*
import io.branchtalk.shared.model.*
import io.scalaland.chimney.dsl.*

import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object PostModels {

  // Post.UrlTitle/Title/Text/CommentsNr are plain neotype newtypes handled uniformly by the Kindlings IsValueType
  // macro-extension. Only Post.URL needs an explicit codec (custom String<->URI representation), and Post.Content
  // needs the kebab-case ADT leaf-name mapper.
  given JsCodec[Post.URL] =
    DefaultJsCodec
      .derived[String]
      .mapDecode[URI](s => Try(URI.create(s)).fold(_ => Left(s"Invalid URI: $s"), Right(_)))(_.toString)
      .asNewtypeCodec[Post.URL]
  // Post.Content uses kebab-case ADT leaf names. Both the codec and the Kindlings tapir-schema read the same locally
  // scoped JsoniterConfig, so the OpenAPI schema stays in sync with the JSON automatically - no separate tapir config.
  given JsCodec[Post.Content] = {
    inline given JsCodecConfig =
      JsCodecConfig().withAdtLeafClassNameMapper(adtDiscriminatorNameMapper).withDiscriminator("type")
    DefaultJsCodec.derived[Post.Content]
  }
  given JsSchema[Post.Content] = {
    inline given JsCodecConfig =
      JsCodecConfig().withAdtLeafClassNameMapper(adtDiscriminatorNameMapper).withDiscriminator("type")
    summon[hearth.kindlings.tapirschemaderivation.KindlingsSchema[Post.Content]].schema
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
