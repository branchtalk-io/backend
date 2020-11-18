package io.branchtalk.discussions.api

import cats.data.NonEmptyList
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.ADT
import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.api.TapirSupport._
import io.branchtalk.discussions.model.Channel
import io.branchtalk.shared.model.{ ID, OptionUpdatable, Updatable }
import io.scalaland.catnip.Semi
import io.scalaland.chimney.dsl._
import sttp.tapir.Schema

@SuppressWarnings(Array("org.wartremover.warts.All")) // for macros
object ChannelModels {

  // properties codecs

  implicit val channelUrlNameCodec: JsCodec[Channel.UrlName] =
    summonCodec[String](JsonCodecMaker.make).refine[MatchesRegex["[A-Za-z0-9_-]+"]].asNewtype[Channel.UrlName]
  implicit val channelNameCodec: JsCodec[Channel.Name] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Channel.Name]
  implicit val channelDescriptionCodec: JsCodec[Channel.Description] =
    summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Channel.Description]

  // properties schemas
  implicit val channelUrlNameSchema: Schema[Channel.UrlName] =
    summonSchema[String Refined MatchesRegex["[A-Za-z0-9_-]+"]].asNewtype[Channel.UrlName]
  implicit val channelNameSchema: Schema[Channel.Name] =
    summonSchema[NonEmptyString].asNewtype[Channel.Name]
  implicit val channelDescriptionSchema: Schema[Channel.Description] =
    summonSchema[NonEmptyString].asNewtype[Channel.Description]

  @Semi(JsCodec) sealed trait ChannelError extends ADT
  object ChannelError {

    @Semi(JsCodec) final case class BadCredentials(msg: String) extends ChannelError
    @Semi(JsCodec) final case class NoPermission(msg: String) extends ChannelError
    @Semi(JsCodec) final case class NotFound(msg: String) extends ChannelError
    @Semi(JsCodec) final case class ValidationFailed(error: NonEmptyList[String]) extends ChannelError
  }

  @Semi(JsCodec) final case class APIChannel(
    id:          ID[Channel],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  )
  object APIChannel {

    def fromDomain(channel: Channel): APIChannel =
      channel.data.into[APIChannel].withFieldConst(_.id, channel.id).transform
  }

  @Semi(JsCodec) final case class CreateChannelRequest(
    name:        Channel.Name,
    description: Option[Channel.Description]
  )

  @Semi(JsCodec) final case class CreateChannelResponse(id: ID[Channel])

  // TODO: unify behavior (Channel sets UrlName while Post generates it)
  @Semi(JsCodec) final case class UpdateChannelRequest(
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description]
  )

  @Semi(JsCodec) final case class UpdateChannelResponse(id: ID[Channel])

  @Semi(JsCodec) final case class DeleteChannelResponse(id: ID[Channel])

  @Semi(JsCodec) final case class RestoreChannelResponse(id: ID[Channel])
}
