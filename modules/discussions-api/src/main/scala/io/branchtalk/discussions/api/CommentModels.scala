package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.discussions.model.{ Channel, Comment, Post, User }
import io.branchtalk.shared.model.{ CreationTime, ID, ModificationTime, Updatable }
import io.scalaland.chimney.dsl.*

object CommentModels {

  // Comment.Content/RepliesNr are plain neotype newtypes handled uniformly by the Kindlings IsValueType
  // macro-extension (neotype-kindlings) - no per-companion codecs needed.

  sealed trait CommentError derives DefaultJsCodec, JsSchema
  object CommentError {

    final case class BadCredentials(msg: String) extends CommentError derives DefaultJsCodec, JsSchema
    final case class NoPermission(msg: String) extends CommentError derives DefaultJsCodec, JsSchema
    final case class NotFound(msg: String) extends CommentError derives DefaultJsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends CommentError derives DefaultJsCodec, JsSchema
  }

  final case class APIComment(
    id:             ID[Comment],
    authorID:       ID[User],
    channelID:      ID[Channel],
    postID:         ID[Post],
    content:        Comment.Content,
    replyTo:        Option[ID[Comment]],
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime],
    repliesNr:      Comment.RepliesNr
  ) derives DefaultJsCodec,
        JsSchema
  object APIComment {

    def fromDomain(comment: Comment): APIComment =
      comment.data.into[APIComment].withFieldConst(_.id, comment.id).transform
  }

  final case class CreateCommentRequest(
    content: Comment.Content,
    replyTo: Option[ID[Comment]]
  ) derives DefaultJsCodec,
        JsSchema

  final case class CreateCommentResponse(id: ID[Comment]) derives DefaultJsCodec, JsSchema

  final case class UpdateCommentRequest(
    newContent: Updatable[Comment.Content]
  ) derives DefaultJsCodec,
        JsSchema

  final case class UpdateCommentResponse(id: ID[Comment]) derives DefaultJsCodec, JsSchema

  final case class DeleteCommentResponse(id: ID[Comment]) derives DefaultJsCodec, JsSchema

  final case class RestoreCommentResponse(id: ID[Comment]) derives DefaultJsCodec, JsSchema
}
