package io.branchtalk

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
import sttp.tapir.{ Codec, Schema }
import sttp.tapir.codec.refined._
import sttp.tapir.CodecFormat.TextPlain

package object api {

  def summonCodec[T](implicit codec:   JsonValueCodec[T]): JsonValueCodec[T] = codec
  def summonSchema[T](implicit schema: Schema[T]):         Schema[T]         = schema

  implicit class RefineJsoniterValueCodec[T](private val codec: JsonValueCodec[T]) extends AnyVal {

    def mapDecode[U](f: T => Either[String, U])(g: U => T): JsonValueCodec[U] = new JsonValueCodec[U] {
      override def decodeValue(in: JsonReader, default: U): U = f(codec.decodeValue(in, g(default))) match {
        case Left(error)  => in.decodeError(error)
        case Right(value) => value
      }

      override def encodeValue(x: U, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null"))
      override def nullValue: U = null.asInstanceOf[U] // scalastyle:ignore
    }

    def refine[P: Validate[T, *]]: JsonValueCodec[T Refined P] = mapDecode(refineV[P](_: T))(_.value)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def asNewtype[N]: JsonValueCodec[N] = codec.asInstanceOf[JsonValueCodec[N]]
  }

  implicit class RefineSchema[T](private val schema: Schema[T]) extends AnyVal {

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def asNewtype[N]: Schema[N] = schema.asInstanceOf[Schema[N]]
  }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def idCodec[A]: Codec[String, ID[A], TextPlain] = Codec.uuid.asInstanceOf[Codec[String, ID[A], TextPlain]]
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def idSchema[A]: Schema[ID[A]] = Schema.schemaForUUID.asNewtype[ID[A]]

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  implicit def idJsoniterValueCodec[A]: JsonValueCodec[ID[A]] =
    summonCodec[UUID](JsonCodecMaker.make).asNewtype[ID[A]]

  @newtype final case class SessionID(value: UUID)
  object SessionID {
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[SessionID] =
      UUID.parse[F](string).map(SessionID(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val jsoniterValueCodec: JsonValueCodec[SessionID] =
      summonCodec[UUID](JsonCodecMaker.make).asNewtype[SessionID]
  }

  @newtype final case class Username(value: NonEmptyString)
  object Username {
    def parse[F[_]: Sync](string: String): F[Username] =
      ParseRefined[F].parse[NonEmpty](string).map(Username(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val jsoniterValueCodec: JsonValueCodec[Username] =
      summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Username]
  }

  @newtype final case class Password(value: NonEmptyString)
  object Password {
    def parse[F[_]: Sync](string: String): F[Password] =
      ParseRefined[F].parse[NonEmpty](string).map(Password(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val jsoniterValueCodec: JsonValueCodec[Password] =
      summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Password]
  }

  @newtype final case class PaginationOffset(value: Int Refined NonNegative)
  object PaginationOffset {
    def parse[F[_]: Sync](int: Int): F[PaginationOffset] =
      ParseRefined[F].parse[NonNegative](int).map(PaginationOffset(_))

    implicit val jsoniterValueCodec: JsonValueCodec[PaginationOffset] =
      summonCodec[Int](JsonCodecMaker.make).refine[NonNegative].asNewtype[PaginationOffset]
    implicit val codec: Codec[String, PaginationOffset, TextPlain] =
      implicitly[Codec[String, Int Refined NonNegative, TextPlain]].map(PaginationOffset(_))(_.value)
    implicit val schema: Schema[PaginationOffset] =
      implicitly[Schema[Int Refined NonNegative]].asNewtype[PaginationOffset]
  }

  @newtype final case class PaginationLimit(value: Int Refined Positive)
  object PaginationLimit {
    def parse[F[_]: Sync](int: Int): F[PaginationLimit] =
      ParseRefined[F].parse[Positive](int).map(PaginationLimit(_))

    implicit val jsoniterValueCodec: JsonValueCodec[PaginationLimit] =
      summonCodec[Int](JsonCodecMaker.make).refine[Positive].asNewtype[PaginationLimit]
    implicit val codec: Codec[String, PaginationLimit, TextPlain] =
      implicitly[Codec[String, Int Refined Positive, TextPlain]].map(PaginationLimit(_))(_.value)
    implicit val schema: Schema[PaginationLimit] =
      summonSchema[Int Refined Positive].asNewtype[PaginationLimit]
  }

  @newtype final case class PaginationHasNext(value: Boolean)
  object PaginationHasNext {

    implicit val jsoniterValueCodec: JsonValueCodec[PaginationHasNext] =
      summonCodec[Boolean](JsonCodecMaker.make).asNewtype[PaginationHasNext]
    implicit val codec: Codec[String, PaginationOffset, TextPlain] =
      implicitly[Codec[String, Int Refined NonNegative, TextPlain]].map(PaginationOffset(_))(_.value)
    implicit val schema: Schema[PaginationHasNext] =
      summonSchema[Int Refined NonNegative].asNewtype[PaginationHasNext]
  }
}
