package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.api.TapirSupport.*
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.{ ID, OptionUpdatable, Updatable }
import io.scalaland.chimney.dsl.*

object ChannelModels {

  // properties codecs

  implicit val channelUrlNameCodec: JsCodec[Channel.UrlName] =
    summonCodec[String](JsonCodecMaker.make).refine[MatchesRegex["[A-Za-z0-9_-]+"]].asNewtype[Channel.UrlName]
  implicit val channelNameCodec: JsCodec[Channel.Name] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Channel.Name]
  implicit val channelDescriptionCodec: JsCodec[Channel.Description] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Channel.Description]

  // properties schemas
  implicit val channelUrlNameSchema: JsSchema[Channel.UrlName] =
    summonSchema[String Refined MatchesRegex["[A-Za-z0-9_-]+"]].asNewtypeSchema[Channel.UrlName]
  implicit val channelNameSchema: JsSchema[Channel.Name] =
    summonSchema[NonEmptyString].asNewtypeSchema[Channel.Name]
  implicit val channelDescriptionSchema: JsSchema[Channel.Description] =
    summonSchema[NonEmptyString].asNewtypeSchema[Channel.Description]

  sealed trait ChannelError derives JsCodec, JsSchema
  object ChannelError {

    final case class BadCredentials(msg: String) extends ChannelError derives JsCodec, JsSchema
    final case class NoPermission(msg: String) extends ChannelError derives JsCodec, JsSchema
    final case class NotFound(msg: String) extends ChannelError derives JsCodec, JsSchema
    final case class ValidationFailed(error: NonEmptyList[String]) extends ChannelError derives JsCodec, JsSchema
  }

  final case class APIChannel(
    id:          ID[Channel],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  ) derives JsCodec,
        JsSchema
  object APIChannel {

    def fromDomain(channel: Channel): APIChannel =
      channel.data.into[APIChannel].withFieldConst(_.id, channel.id).transform
  }

  final case class CreateChannelRequest(
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  ) derives JsCodec,
        JsSchema

  final case class CreateChannelResponse(id: ID[Channel]) derives JsCodec, JsSchema

  // TODO: unify behavior (Channel sets UrlName while Post generates it)
  final case class UpdateChannelRequest(
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description]
  ) derives JsCodec,
        JsSchema

  final case class UpdateChannelResponse(id: ID[Channel]) derives JsCodec, JsSchema

  final case class DeleteChannelResponse(id: ID[Channel]) derives JsCodec, JsSchema

  final case class RestoreChannelResponse(id: ID[Channel]) derives JsCodec, JsSchema
}
