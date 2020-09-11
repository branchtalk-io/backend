package io.branchtalk

import cats.effect.Sync
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.models.{ ID, ParseRefined, UUID, UUIDGenerator }
import io.estatico.newtype.macros.newtype
import sttp.tapir.{ Codec, Mapping, Schema }
import sttp.tapir.CodecFormat.TextPlain

package object api {

  def idMapped[A]:         Mapping[UUID, ID[A]]            = Mapping.from[UUID, ID[A]](string => ID[A](string))(_.value)
  implicit def idCodec[A]: Codec[String, ID[A], TextPlain] = Codec.uuid.map(idMapped)
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def idSchema[A]: Schema[ID[A]] = implicitly[Schema[UUID]].asInstanceOf[Schema[ID[A]]]

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null"))
  implicit def idJsoniterValueCodec[A]: JsonValueCodec[ID[A]] =
    JsonCodecMaker.make[UUID].asInstanceOf[JsonValueCodec[ID[A]]]

  @newtype final case class SessionID(value: UUID)
  object SessionID {
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[SessionID] =
      UUID.parse[F](string).map(SessionID.apply)
  }

  @newtype final case class Username(value: NonEmptyString)
  object Username {
    def parse[F[_]: Sync](string: String): F[Username] =
      ParseRefined[F].parse[NonEmpty](string).map(Username.apply)
  }

  @newtype final case class Password(value: NonEmptyString)
  object Password {
    def parse[F[_]: Sync](string: String): F[Password] =
      ParseRefined[F].parse[NonEmpty](string).map(Password.apply)
  }
}
