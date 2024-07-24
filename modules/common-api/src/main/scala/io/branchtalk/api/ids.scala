package io.branchtalk.api

import cats.{ Eq, Show }
import cats.effect.Sync
import io.branchtalk.shared.model.{ ParseNewtype, UUID }
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.api.TapirSupport.*
import neotype.*

type SessionID = SessionID.Type
object SessionID extends Newtype[UUID] {

  def unapply(sessionID: SessionID): Some[UUID] = Some(sessionID.unwrap)
  def parse[F[_]: Sync](string: String)(using UUID.Generator): F[SessionID] = UUID.parse[F](string).map(unsafeMake)

  given Eq[SessionID]       = unsafeMakeF[Eq](Eq.fromUniversalEquals[UUID])
  given Show[SessionID]     = unsafeMakeF[Show](Show[UUID])
  given JsCodec[SessionID]  = DefaultJsCodec.derived[UUID].asNewtypeCodec[SessionID]
  given JsSchema[SessionID] = summonSchema[UUID].asNewtypeSchema[SessionID]
}

type UserID = UserID.Type
object UserID extends Newtype[UUID] {

  val empty: UserID = unsafeMake(UUID.empty)

  def unapply(userID: UserID): Some[UUID] = Some(userID.unwrap)
  def parse[F[_]: Sync](string: String)(using UUID.Generator): F[UserID] = UUID.parse[F](string).map(unsafeMake)

  given Eq[UserID]       = unsafeMakeF[Eq](Eq.fromUniversalEquals[UUID])
  given Show[UserID]     = unsafeMakeF[Show](Show[UUID])
  given JsCodec[UserID]  = DefaultJsCodec.derived[UUID].asNewtypeCodec[UserID]
  given JsSchema[UserID] = summonSchema[UUID].asNewtypeSchema[UserID]
}

type ChannelID = ChannelID.Type
object ChannelID extends Newtype[UUID] {

  val empty: ChannelID = unsafeMake(UUID.empty)

  def unapply(channelID: ChannelID): Some[UUID] = Some(channelID.unwrap)
  def parse[F[_]: Sync](string: String)(using UUID.Generator): F[ChannelID] = UUID.parse[F](string).map(unsafeMake)

  given Eq[ChannelID]       = unsafeMakeF[Eq](Eq.fromUniversalEquals[UUID])
  given Show[ChannelID]     = unsafeMakeF[Show](Show[UUID])
  given JsCodec[ChannelID]  = DefaultJsCodec.derived[UUID].asNewtypeCodec[ChannelID]
  given JsSchema[ChannelID] = summonSchema[UUID].asNewtypeSchema[ChannelID]
}
