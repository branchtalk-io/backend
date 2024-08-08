package io.branchtalk.api

import cats.{ Eq, Show }
import cats.effect.Sync
import io.branchtalk.shared.model.ParseNewtype
import io.branchtalk.api.JsoniterSupport.*
import io.branchtalk.api.TapirSupport.*

type Username = Username.Type
object Username extends Newtype[String] {

  override inline def validate(input: String): Boolean = input.nonEmpty

  def unapply(username: Username): Some[String] = Some(username.unwrap)
  def parse[F[_]: Sync](string: String): F[Username] = ParseNewtype[F].parse[Username](string)

  given Eq[Username]       = unsafeMakeF[Eq](Eq[String])
  given Show[Username]     = unsafeMakeF[Show](Show[String])
  given JsCodec[Username]  = DefaultJsCodec.derived[String].asNewtypeCodec[Username]
  given JsSchema[Username] = summonSchema[String].asNewtypeSchema[Username]
}

type Password = Password.Type
object Password extends Newtype[Array[Byte]] {

  override inline def validate(input: Array[Byte]): Boolean = input.nonEmpty

  def unapply(password: Password): Some[Array[Byte]] = Some(password.unwrap)
  def parse[F[_]: Sync](array: Array[Byte]): F[Password] = ParseNewtype[F].parse[Password](array)

  given JsCodec[Password]  = DefaultJsCodec.derived[Array[Byte]].asNewtypeCodec[Password]
  given JsSchema[Password] = summonSchema[Array[Byte]].asNewtypeSchema[Password]
}
