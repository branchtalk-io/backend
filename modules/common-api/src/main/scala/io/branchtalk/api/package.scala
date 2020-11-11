package io.branchtalk

import java.net.URI

import cats.Id
import cats.data.{ Chain, NonEmptyChain, NonEmptyList }
import cats.effect.Sync
import com.github.plokhotnyuk.jsoniter_scala.core.{ JsonReader, JsonValueCodec, JsonWriter }
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined._
import eu.timepit.refined.api.{ Refined, Validate }
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.models.{ ID, OptionUpdatable, ParseRefined, UUID, UUIDGenerator, Updatable }
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.Coercible
import sttp.tapir.{ Codec, DecodeResult, Schema }
import sttp.tapir.codec.refined._
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.generic.Configuration

// scalastyle:off number.of.methods
package object api {

  implicit class TapirResultOps[A](private val decodeResult: DecodeResult[A]) extends AnyVal {

    def toOption: Option[A] = decodeResult match {
      case DecodeResult.Value(v) => v.some
      case _                     => none[A]
    }
  }

  // shortcuts
  type JsCodec[A] = JsonValueCodec[A]
  type Param[A]   = Codec[String, A, TextPlain]

  def summonCodec[T](implicit codec:   JsCodec[T]): JsCodec[T] = codec
  def summonParam[T](implicit param:   Param[T]):   Param[T]   = param
  def summonSchema[T](implicit schema: Schema[T]):  Schema[T]  = schema

  @SuppressWarnings(Array("org.wartremover.warts.All")) // handling valid null values
  implicit class RefineCodec[T](private val codec: JsCodec[T]) extends AnyVal {

    def mapDecode[U](f: T => Either[String, U])(g: U => T): JsCodec[U] = new JsCodec[U] {
      override def decodeValue(in: JsonReader, default: U): U =
        codec.decodeValue(in, if (default != null) g(default) else null.asInstanceOf[T]) match { // scalastyle:ignore
          case null => null.asInstanceOf[U] // scalastyle:ignore
          case t =>
            f(t) match {
              case null         => null.asInstanceOf[U] // scalastyle:ignore
              case Left(error)  => in.decodeError(error)
              case Right(value) => value
            }
        }

      override def encodeValue(x: U, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      override def nullValue: U = codec.nullValue match {
        case null => null.asInstanceOf[U] // scalastyle:ignore
        case u    => f(u).getOrElse(null.asInstanceOf[U]) // scalastyle:ignore
      }
    }

    def map[U](f: T => U)(g: U => T): JsCodec[U] = new JsCodec[U] {
      override def decodeValue(in: JsonReader, default: U): U =
        codec.decodeValue(in, if (default != null) g(default) else null.asInstanceOf[T]) match { // scalastyle:ignore
          case null => null.asInstanceOf[U] // scalastyle:ignore
          case t    => f(t)
        }

      override def encodeValue(x: U, out: JsonWriter): Unit = codec.encodeValue(g(x), out)

      override def nullValue: U = codec.nullValue match {
        case null => null.asInstanceOf[U] // scalastyle:ignore
        case u    => f(u)
      }
    }

    def refine[P: Validate[T, *]]: JsCodec[T Refined P] = mapDecode(refineV[P](_: T))(_.value)

    def asNewtype[N: Coercible[T, *]]: JsCodec[N] = Coercible.unsafeWrapMM[JsCodec, Id, T, N].apply(codec)
  }

  implicit class RefineSchema[T](private val schema: Schema[T]) extends AnyVal {

    def asNewtype[N: Coercible[T, *]]: Schema[N] = Coercible.unsafeWrapMM[Schema, Id, T, N].apply(schema)
  }

  implicit val uriSchema: Schema[URI] = Schema.schemaForString.asInstanceOf[Schema[URI]]

  // domain instances

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  implicit def idCodec[A]:  JsCodec[ID[A]] = summonCodec[UUID](JsonCodecMaker.make).asNewtype[ID[A]]
  implicit def idParam[A]:  Param[ID[A]]   = summonParam[UUID].map[ID[A]](ID[A](_))(_.uuid)
  implicit def idSchema[A]: Schema[ID[A]]  = summonSchema[UUID].asNewtype[ID[A]]

  implicit def updatableCodec[A: JsCodec]: JsCodec[Updatable[A]] = summonCodec[Updatable[A]](
    JsonCodecMaker.make(CodecMakerConfig.withAdtLeafClassNameMapper(JsonCodecMaker.`enforce-kebab-case`))
  )
  implicit def updatableSchema[A: Schema]: Schema[Updatable[A]] = {
    implicit val customConfiguration: Configuration = Configuration.default.withKebabCaseMemberNames
    Schema.derivedSchema[Updatable[A]]
  }

  implicit def optionUpdatableCodec[A: JsCodec]: JsCodec[OptionUpdatable[A]] = summonCodec[OptionUpdatable[A]](
    JsonCodecMaker.make(CodecMakerConfig.withAdtLeafClassNameMapper(JsonCodecMaker.`enforce-kebab-case`))
  )
  implicit def optionUpdatableSchema[A: Schema]: Schema[OptionUpdatable[A]] = {
    implicit val customConfiguration: Configuration = Configuration.default.withKebabCaseMemberNames
    Schema.derivedSchema[OptionUpdatable[A]]
  }

  /// Cats codecs

  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit def chainCodec[A: JsCodec]: JsCodec[Chain[A]] =
    summonCodec[List[A]](JsonCodecMaker.make).map(Chain.fromSeq)(_.toList)
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def chainSchema[A: Schema]: Schema[Chain[A]] =
    summonSchema[List[A]].asInstanceOf[Schema[Chain[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit def necCodec[A: JsCodec]: JsCodec[NonEmptyChain[A]] = summonCodec[List[A]](JsonCodecMaker.make).mapDecode {
    case head :: tail => NonEmptyChain(head, tail: _*).asRight[String]
    case _            => "Expected non-empty list".asLeft[NonEmptyChain[A]]
  }(_.toList)
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def necSchema[A: Schema]: Schema[NonEmptyChain[A]] =
    summonSchema[List[A]].asInstanceOf[Schema[NonEmptyChain[A]]] // scalastyle:ignore

  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit def nelCodec[A: JsCodec]: JsCodec[NonEmptyList[A]] = summonCodec[List[A]](JsonCodecMaker.make).mapDecode {
    case head :: tail => NonEmptyList(head, tail).asRight[String]
    case _            => "Expected non-empty list".asLeft[NonEmptyList[A]]
  }(_.toList)
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit def nelSchema[A: Schema]: Schema[NonEmptyList[A]] =
    summonSchema[List[A]].asInstanceOf[Schema[NonEmptyList[A]]] // scalastyle:ignore

  // API definitions and instances

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
      summonCodec[String](JsonCodecMaker.make).map(_.getBytes)(new String(_)).refine[NonEmpty].asNewtype[Password]
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
