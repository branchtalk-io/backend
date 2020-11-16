package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.ADT
import io.branchtalk.api._
import io.branchtalk.discussions.model.Channel
import io.scalaland.catnip.Semi

object ChannelModels {

  // properties codecs

  // properties schemas

  @Semi(JsCodec) sealed trait PostError extends ADT
  object PostError {

    @Semi(JsCodec) final case class BadCredentials(msg: String) extends PostError
    @Semi(JsCodec) final case class NoPermission(msg: String) extends PostError
    @Semi(JsCodec) final case class NotFound(msg: String) extends PostError
    @Semi(JsCodec) final case class ValidationFailed(error: NonEmptyList[String]) extends PostError
  }

  @Semi(JsCodec) final case class APIChannel()
  object APIChannel {

    def fromDomain(channel: Channel): APIChannel = APIChannel()
  }
}
