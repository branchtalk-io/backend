package io.branchtalk

import java.net.URI

import cats.effect.Sync
import com.github.plokhotnyuk.jsoniter_scala.core.{ JsonReader, JsonValueCodec, JsonWriter }
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined._
import eu.timepit.refined.api.{ Refined, Validate }
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.models.{ ID, ParseRefined, UUID, UUIDGenerator }
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.Coercible
import sttp.tapir.{ Codec, Schema }
import sttp.tapir.codec.refined._
import sttp.tapir.CodecFormat.TextPlain

package object api {

  // shortcuts
  type JsCodec[A] = JsonValueCodec[A]
  type Param[A]   = Codec[String, A, TextPlain]

  def summonCodec[T](implicit codec:   JsCodec[T]): JsCodec[T] = codec
  def summonParam[T](implicit param:   Param[T]):   Param[T]   = param
  def summonSchema[T](implicit schema: Schema[T]):  Schema[T]  = schema

  implicit class RefineCodec[T](private val codec: JsCodec[T]) extends AnyVal {

    def mapDecode[U](f: T => Either[String, U])(g: U => T): JsCodec[U] = new JsCodec[U] {
      override def decodeValue(in: JsonReader, default: U): U = f(codec.decodeValue(in, g(default))) match {
        case Left(error)  => in.decodeError(error)
        case Right(value) => value
      }

      override def encodeValue(x: U, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null"))
      override def nullValue: U = null.asInstanceOf[U] // scalastyle:ignore
    }

    def map[U](f: T => U)(g: U => T): JsCodec[U] = new JsCodec[U] {
      override def decodeValue(in: JsonReader, default: U): U = f(codec.decodeValue(in, g(default)))

      override def encodeValue(x: U, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null"))
      override def nullValue: U = null.asInstanceOf[U] // scalastyle:ignore
    }

    def refine[P: Validate[T, *]]: JsCodec[T Refined P] = mapDecode(refineV[P](_: T))(_.value)

    def asNewtype[N](implicit ev: Coercible[JsCodec[T], JsCodec[N]]): JsCodec[N] = ev(codec)
  }

  implicit class RefineSchema[T](private val schema: Schema[T]) extends AnyVal {

    def asNewtype[N](implicit ev: Coercible[Schema[T], Schema[N]]): Schema[N] = ev(schema)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  implicit def idCodec[A]:  JsCodec[ID[A]] = summonCodec[UUID](JsonCodecMaker.make).asNewtype[ID[A]]
  implicit def idParam[A]:  Param[ID[A]]   = summonParam[UUID].map[ID[A]](ID[A](_))(_.uuid)
  implicit def idSchema[A]: Schema[ID[A]]  = summonSchema[UUID].asNewtype[ID[A]]

  implicit val uriSchema: Schema[URI] = Schema.schemaForString.asInstanceOf[Schema[URI]]

  // TODO: implement codec for non-empty list/chain from cats because the derived is treating NEL as case class

  @newtype final case class SessionID(uuid: UUID)
  object SessionID {
    def unapply(sessionID: SessionID): Option[UUID] = sessionID.uuid.some
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[SessionID] =
      UUID.parse[F](string).map(SessionID(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec:  JsCodec[SessionID] = summonCodec[UUID](JsonCodecMaker.make).asNewtype[SessionID]
    implicit val schema: Schema[SessionID]  = summonSchema[UUID].asNewtype[SessionID]
  }

  @newtype final case class UserID(uuid: UUID)
  object UserID {
    def unapply(userID: UserID): Option[UUID] = userID.uuid.some
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[UserID] =
      UUID.parse[F](string).map(UserID(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec:  JsCodec[UserID] = summonCodec[UUID](JsonCodecMaker.make).asNewtype[UserID]
    implicit val schema: Schema[UserID]  = summonSchema[UUID].asNewtype[UserID]
  }

  @newtype final case class ChannelID(uuid: UUID)
  object ChannelID {
    def unapply(channelID: ChannelID): Option[UUID] = channelID.uuid.some
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[ChannelID] =
      UUID.parse[F](string).map(ChannelID(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec:  JsCodec[ChannelID] = summonCodec[UUID](JsonCodecMaker.make).asNewtype[ChannelID]
    implicit val schema: Schema[ChannelID]  = summonSchema[UUID].asNewtype[ChannelID]
  }

  @newtype final case class Username(nonEmptyString: NonEmptyString)
  object Username {
    def unapply(username: Username): Option[NonEmptyString] = username.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Username] =
      ParseRefined[F].parse[NonEmpty](string).map(Username(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec: JsCodec[Username] =
      summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Username]
    implicit val schema: Schema[Username] =
      summonSchema[String Refined NonEmpty].asNewtype[Username]
  }

  @newtype final case class Password(nonEmptyBytes: Array[Byte] Refined NonEmpty)
  object Password {
    def unapply(password: Password): Option[Array[Byte] Refined NonEmpty] = password.nonEmptyBytes.some
    def parse[F[_]: Sync](bytes: Array[Byte]): F[Password] =
      ParseRefined[F].parse[NonEmpty](bytes).map(Password(_))

    @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
    implicit val codec: JsCodec[Password] =
      summonCodec[Array[Byte]](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Password]
    implicit val schema: Schema[Password] =
      summonSchema[Array[Byte] Refined NonEmpty].asNewtype[Password]
  }

  @newtype final case class PaginationOffset(nonNegativeLong: Long Refined NonNegative)
  object PaginationOffset {
    def unapply(offset: PaginationOffset): Option[Long Refined NonNegative] = offset.nonNegativeLong.some
    def parse[F[_]: Sync](long: Long): F[PaginationOffset] =
      ParseRefined[F].parse[NonNegative](long).map(PaginationOffset(_))

    implicit val codec: JsCodec[PaginationOffset] =
      summonCodec[Long](JsonCodecMaker.make).refine[NonNegative].asNewtype[PaginationOffset]
    implicit val param: Param[PaginationOffset] =
      summonParam[Long Refined NonNegative].map(PaginationOffset(_))(_.nonNegativeLong)
    implicit val schema: Schema[PaginationOffset] =
      summonSchema[Long Refined NonNegative].asNewtype[PaginationOffset]
  }

  @newtype final case class PaginationLimit(positiveInt: Int Refined Positive)
  object PaginationLimit {
    def unapply(limit: PaginationLimit): Option[Int Refined Positive] = limit.positiveInt.some
    def parse[F[_]: Sync](int: Int): F[PaginationLimit] =
      ParseRefined[F].parse[Positive](int).map(PaginationLimit(_))

    implicit val codec: JsCodec[PaginationLimit] =
      summonCodec[Int](JsonCodecMaker.make).refine[Positive].asNewtype[PaginationLimit]
    implicit val param: Param[PaginationLimit] =
      summonParam[Int Refined Positive].map(PaginationLimit(_))(_.positiveInt)
    implicit val schema: Schema[PaginationLimit] =
      summonSchema[Int Refined Positive].asNewtype[PaginationLimit]
  }

  @newtype final case class PaginationHasNext(bool: Boolean)
  object PaginationHasNext {
    def unapply(hasNext: PaginationHasNext): Option[Boolean] = hasNext.bool.some

    implicit val codec: JsCodec[PaginationHasNext] =
      summonCodec[Boolean](JsonCodecMaker.make).asNewtype[PaginationHasNext]
    implicit val schema: Schema[PaginationHasNext] =
      summonSchema[Boolean].asNewtype[PaginationHasNext]
  }
}
