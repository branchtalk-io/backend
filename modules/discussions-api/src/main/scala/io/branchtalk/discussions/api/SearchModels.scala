package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object SearchModels {

  sealed trait SearchError derives DefaultJsCodec, JsSchema
  object SearchError {

    final case class BadCredentials(msg: String) extends SearchError derives DefaultJsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends SearchError derives DefaultJsCodec, JsSchema
  }
}
