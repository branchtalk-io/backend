package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import io.branchtalk.api.JsoniterSupport.{ *, given }
import io.branchtalk.api.TapirSupport.{ *, given }
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.{ CreationTime, ID, ModificationTime, OptionUpdatable, Updatable }
import io.scalaland.chimney.dsl.*

object ChannelModels {

  // Channel.UrlName/Name/Description are plain neotype newtypes handled uniformly by the Kindlings IsValueType
  // macro-extension (neotype-kindlings) - no per-companion codecs needed.

  sealed trait ChannelError derives DefaultJsCodec, JsSchema
  object ChannelError {

    final case class BadCredentials(msg: String) extends ChannelError derives DefaultJsCodec, JsSchema
    final case class NoPermission(msg: String) extends ChannelError derives DefaultJsCodec, JsSchema
    final case class NotFound(msg: String) extends ChannelError derives DefaultJsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends ChannelError derives DefaultJsCodec, JsSchema
  }

  final case class APIChannel(
    id:             ID[Channel],
    urlName:        Channel.UrlName,
    name:           Channel.Name,
    description:    Option[Channel.Description],
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  ) derives DefaultJsCodec,
        JsSchema
  object APIChannel {

    def fromDomain(channel: Channel): APIChannel =
      channel.data.into[APIChannel].withFieldConst(_.id, channel.id).transform
  }

  final case class CreateChannelRequest(
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  ) derives DefaultJsCodec,
        JsSchema

  final case class CreateChannelResponse(id: ID[Channel]) derives DefaultJsCodec, JsSchema

  final case class UpdateChannelRequest(
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description]
  ) derives DefaultJsCodec,
        JsSchema

  final case class UpdateChannelResponse(id: ID[Channel]) derives DefaultJsCodec, JsSchema

  final case class DeleteChannelResponse(id: ID[Channel]) derives DefaultJsCodec, JsSchema

  final case class RestoreChannelResponse(id: ID[Channel]) derives DefaultJsCodec, JsSchema
}
