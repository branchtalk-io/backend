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
import sttp.tapir.{ Codec, Mapping, Schema }
import sttp.tapir.CodecFormat.TextPlain

package object api {

  def idMapped[A]:         Mapping[UUID, ID[A]]            = Mapping.from[UUID, ID[A]](string => ID[A](string))(_.value)
  implicit def idCodec[A]: Codec[String, ID[A], TextPlain] = Codec.uuid.map(idMapped)
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def idSchema[A]: Schema[ID[A]] = implicitly[Schema[UUID]].asInstanceOf[Schema[ID[A]]]

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

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  implicit def idJsoniterValueCodec[A]: JsonValueCodec[ID[A]] = JsonCodecMaker.make[UUID].asNewtype[ID[A]]

  @newtype final case class SessionID(value: UUID)
  object SessionID {
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[SessionID] =
      UUID.parse[F](string).map(SessionID.apply)

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val sessionIDJsoniterValueCodec: JsonValueCodec[SessionID] =
      JsonCodecMaker.make[UUID].asNewtype[SessionID]
  }

  @newtype final case class Username(value: NonEmptyString)
  object Username {
    def parse[F[_]: Sync](string: String): F[Username] =
      ParseRefined[F].parse[NonEmpty](string).map(Username.apply)

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val usernameJsoniterValueCodec: JsonValueCodec[Username] =
      JsonCodecMaker.make[String].refine[NonEmpty].asNewtype[Username]
  }

  @newtype final case class Password(value: NonEmptyString)
  object Password {
    def parse[F[_]: Sync](string: String): F[Password] =
      ParseRefined[F].parse[NonEmpty](string).map(Password.apply)

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val passwordJsoniterValueCodec: JsonValueCodec[Password] =
      JsonCodecMaker.make[String].refine[NonEmpty].asNewtype[Password]
  }

  @newtype final case class PaginationOffset(value: Int Refined NonNegative)
  object PaginationOffset {
    def parse[F[_]: Sync](int: Int): F[PaginationOffset] =
      ParseRefined[F].parse[NonNegative](int).map(PaginationOffset.apply)

    implicit val offsetJsoniterValueCodec: JsonValueCodec[PaginationOffset] =
      JsonCodecMaker.make[Int].refine[NonNegative].asNewtype[PaginationOffset]
  }

  @newtype final case class PaginationLimit(value: Int Refined Positive)
  object PaginationLimit {
    def parse[F[_]: Sync](int: Int): F[PaginationLimit] =
      ParseRefined[F].parse[Positive](int).map(PaginationLimit.apply)

    implicit val offsetJsoniterValueCodec: JsonValueCodec[PaginationLimit] =
      JsonCodecMaker.make[Int].refine[Positive].asNewtype[PaginationLimit]
  }

  @newtype final case class PaginationHasNext(value: Boolean)
  object PaginationHasNext {

    implicit val offsetJsoniterValueCodec: JsonValueCodec[PaginationHasNext] =
      JsonCodecMaker.make[Boolean].asNewtype[PaginationHasNext]
  }
}
