package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.api.TapirSupport.*
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.ID

object SubscriptionModels {

  sealed trait SubscriptionError derives JsCodec, JsSchema
  object SubscriptionError {

    final case class BadCredentials(msg: String) extends SubscriptionError derives JsCodec, JsSchema
    final case class NoPermission(msg: String) extends SubscriptionError derives JsCodec, JsSchema
    final case class NotFound(msg: String) extends SubscriptionError derives JsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends SubscriptionError derives JsCodec, JsSchema
  }

  final case class APISubscriptions(channels: List[ID[Channel]]) derives JsCodec, JsSchema

  final case class SubscribeRequest(channels: List[ID[Channel]]) derives JsCodec, JsSchema
  final case class SubscribeResponse(channels: List[ID[Channel]]) derives JsCodec, JsSchema

  final case class UnsubscribeRequest(channels: List[ID[Channel]]) derives JsCodec, JsSchema
  final case class UnsubscribeResponse(channels: List[ID[Channel]]) derives JsCodec, JsSchema
}
