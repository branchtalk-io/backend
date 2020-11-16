package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.ADT
import io.branchtalk.api.JsCodec
import io.scalaland.catnip.Semi

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object CommentModels {

  @Semi(JsCodec) sealed trait CommentError extends ADT
  object CommentError {

    @Semi(JsCodec) final case class BadCredentials(msg: String) extends CommentError
    @Semi(JsCodec) final case class NoPermission(msg: String) extends CommentError
    @Semi(JsCodec) final case class NotFound(msg: String) extends CommentError
    @Semi(JsCodec) final case class ValidationFailed(error: NonEmptyList[String]) extends CommentError
  }

  @Semi(JsCodec) final case class APIComment()

  @Semi(JsCodec) final case class CreateCommentRequest()

  @Semi(JsCodec) final case class CreateCommentResponse()

  @Semi(JsCodec) final case class UpdateCommentRequest()

  @Semi(JsCodec) final case class UpdateCommentResponse()

  @Semi(JsCodec) final case class DeleteCommentRequest()

  @Semi(JsCodec) final case class DeleteCommentResponse()

  @Semi(JsCodec) final case class RestoreCommentRequest()

  @Semi(JsCodec) final case class RestoreCommentResponse()
}
