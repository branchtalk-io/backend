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

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def idCodec[A]: Codec[String, ID[A], TextPlain] = Codec.uuid.asInstanceOf[Codec[String, ID[A], TextPlain]]
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def idSchema[A]: Schema[ID[A]] = Schema.schemaForUUID.asInstanceOf[Schema[ID[A]]]

  implicit class RefineJsoniterValueCodec[T](private val codec: JsonValueCodec[T]) extends AnyVal {

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def asNewtype[N]: JsonValueCodec[N] = codec.asInstanceOf[JsonValueCodec[N]]

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    def refine[P: Validate[T, *]]: JsonValueCodec[T Refined P] = new JsonValueCodec[Refined[T, P]] {
      override def decodeValue(in: JsonReader, default: T Refined P): Refined[T, P] =
        refineV[P](codec.decodeValue(in, default.value)) match {
          case Left(error)  => in.decodeError(error)
          case Right(value) => value
        }

      override def encodeValue(x: T Refined P, out: JsonWriter): Unit = codec.encodeValue(x.value, out)

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Null"))
      override def nullValue: T Refined P = null.asInstanceOf[T Refined P] // scalastyle:ignore
    }
  }

  implicit class RefineSchema[T](private val schema: Schema[T]) extends AnyVal {

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def asNewtype[N]: Schema[N] = schema.asInstanceOf[Schema[N]]
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  implicit def idJsoniterValueCodec[A]: JsonValueCodec[ID[A]] = JsonCodecMaker.make[UUID].asNewtype[ID[A]]

  @newtype final case class SessionID(value: UUID)
  object SessionID {
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[SessionID] =
      UUID.parse[F](string).map(SessionID(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val jsoniterValueCodec: JsonValueCodec[SessionID] = JsonCodecMaker.make[UUID].asNewtype[SessionID]
  }

  @newtype final case class Username(value: NonEmptyString)
  object Username {
    def parse[F[_]: Sync](string: String): F[Username] =
      ParseRefined[F].parse[NonEmpty](string).map(Username(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val jsoniterValueCodec: JsonValueCodec[Username] =
      JsonCodecMaker.make[String].refine[NonEmpty].asNewtype[Username]
  }

  @newtype final case class Password(value: NonEmptyString)
  object Password {
    def parse[F[_]: Sync](string: String): F[Password] =
      ParseRefined[F].parse[NonEmpty](string).map(Password(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val jsoniterValueCodec: JsonValueCodec[Password] =
      JsonCodecMaker.make[String].refine[NonEmpty].asNewtype[Password]
  }

  @newtype final case class PaginationOffset(value: Int Refined NonNegative)
  object PaginationOffset {
    def parse[F[_]: Sync](int: Int): F[PaginationOffset] =
      ParseRefined[F].parse[NonNegative](int).map(PaginationOffset(_))

    implicit val jsoniterValueCodec: JsonValueCodec[PaginationOffset] =
      JsonCodecMaker.make[Int].refine[NonNegative].asNewtype[PaginationOffset]
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
      JsonCodecMaker.make[Int].refine[Positive].asNewtype[PaginationLimit]
    implicit val codec: Codec[String, PaginationLimit, TextPlain] =
      implicitly[Codec[String, Int Refined Positive, TextPlain]].map(PaginationLimit(_))(_.value)
    implicit val schema: Schema[PaginationLimit] = implicitly[Schema[Int Refined Positive]].asNewtype[PaginationLimit]
  }

  @newtype final case class PaginationHasNext(value: Boolean)
  object PaginationHasNext {

    implicit val jsoniterValueCodec: JsonValueCodec[PaginationHasNext] =
      JsonCodecMaker.make[Boolean].asNewtype[PaginationHasNext]
    implicit val codec: Codec[String, PaginationOffset, TextPlain] =
      implicitly[Codec[String, Int Refined NonNegative, TextPlain]].map(PaginationOffset(_))(_.value)
    implicit val schema: Schema[PaginationHasNext] =
      implicitly[Schema[Int Refined NonNegative]].asNewtype[PaginationHasNext]
  }
}
