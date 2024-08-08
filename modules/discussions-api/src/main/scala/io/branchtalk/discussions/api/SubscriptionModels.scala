package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.ID

object SubscriptionModels {

  sealed trait SubscriptionError derives DefaultJsCodec, JsSchema
  object SubscriptionError {

    final case class BadCredentials(msg: String) extends SubscriptionError derives DefaultJsCodec, JsSchema
    final case class NoPermission(msg: String) extends SubscriptionError derives DefaultJsCodec, JsSchema
    final case class NotFound(msg: String) extends SubscriptionError derives DefaultJsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends SubscriptionError
        derives DefaultJsCodec,
          JsSchema
  }

  final case class APISubscriptions(channels: List[ID[Channel]]) derives DefaultJsCodec, JsSchema

  final case class SubscribeRequest(channels: List[ID[Channel]]) derives DefaultJsCodec, JsSchema
  final case class SubscribeResponse(channels: List[ID[Channel]]) derives DefaultJsCodec, JsSchema

  final case class UnsubscribeRequest(channels: List[ID[Channel]]) derives DefaultJsCodec, JsSchema
  final case class UnsubscribeResponse(channels: List[ID[Channel]]) derives DefaultJsCodec, JsSchema
}
