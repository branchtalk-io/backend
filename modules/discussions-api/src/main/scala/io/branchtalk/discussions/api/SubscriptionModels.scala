package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.ADT
import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.ID
import io.scalaland.catnip.Semi

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object SubscriptionModels {

  @Semi(JsCodec) sealed trait SubscriptionError extends ADT
  object SubscriptionError {

    @Semi(JsCodec) final case class BadCredentials(msg: String) extends SubscriptionError
    @Semi(JsCodec) final case class NoPermission(msg: String) extends SubscriptionError
    @Semi(JsCodec) final case class NotFound(msg: String) extends SubscriptionError
    @Semi(JsCodec) final case class ValidationFailed(error: NonEmptyList[String]) extends SubscriptionError
  }

  @Semi(JsCodec) final case class APISubscriptions(channels: List[ID[Channel]])

  @Semi(JsCodec) final case class SubscribeRequest(channels: List[ID[Channel]])

  @Semi(JsCodec) final case class SubscribeResponse(channels: List[ID[Channel]])

  @Semi(JsCodec) final case class UnsubscribeRequest(channels: List[ID[Channel]])

  @Semi(JsCodec) final case class UnsubscribeResponse(channels: List[ID[Channel]])
}
