package io.branchtalk

import cats.{ Eq, Show }
import cats.effect.Sync
import com.github.plokhotnyuk.jsoniter_scala.macros._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.{ NonNegative, Positive }
import eu.timepit.refined.types.string.NonEmptyString
import io.branchtalk.shared.model._
import io.branchtalk.api.JsoniterSupport._
import io.branchtalk.api.TapirSupport._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

// scalastyle:off number.of.methods
package object api {

  // API definitions and instances

  @newtype final case class SessionID(uuid: UUID)
  object SessionID {
    def unapply(sessionID: SessionID): Option[UUID] = sessionID.uuid.some
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[SessionID] =
      UUID.parse[F](string).map(SessionID(_))

    implicit val eq:   Eq[SessionID]   = Eq[UUID].coerce
    implicit val show: Show[SessionID] = Show[UUID].coerce
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec:  JsCodec[SessionID]  = summonCodec[UUID](JsonCodecMaker.make).asNewtype[SessionID]
    implicit val schema: JsSchema[SessionID] = summonSchema[UUID].asNewtype[SessionID]
  }

  @newtype final case class UserID(uuid: UUID)
  object UserID {
    def unapply(userID: UserID): Option[UUID] = userID.uuid.some
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[UserID] =
      UUID.parse[F](string).map(UserID(_))

    val empty: UserID = UserID(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))

    implicit val eq:   Eq[UserID]   = Eq[UUID].coerce
    implicit val show: Show[UserID] = Show[UUID].coerce
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec:  JsCodec[UserID]  = summonCodec[UUID](JsonCodecMaker.make).asNewtype[UserID]
    implicit val schema: JsSchema[UserID] = summonSchema[UUID].asNewtype[UserID]
  }

  @newtype final case class ChannelID(uuid: UUID)
  object ChannelID {
    def unapply(channelID: ChannelID): Option[UUID] = channelID.uuid.some
    def parse[F[_]: Sync](string: String)(implicit uuidGenerator: UUIDGenerator): F[ChannelID] =
      UUID.parse[F](string).map(ChannelID(_))

    implicit val eq:   Eq[ChannelID]   = Eq[UUID].coerce
    implicit val show: Show[ChannelID] = Show[UUID].coerce
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec:  JsCodec[ChannelID]  = summonCodec[UUID](JsonCodecMaker.make).asNewtype[ChannelID]
    implicit val schema: JsSchema[ChannelID] = summonSchema[UUID].asNewtype[ChannelID]
  }

  @newtype final case class Username(nonEmptyString: NonEmptyString)
  object Username {
    def unapply(username: Username): Option[NonEmptyString] = username.nonEmptyString.some
    def parse[F[_]: Sync](string: String): F[Username] =
      ParseRefined[F].parse[NonEmpty](string).map(Username(_))

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    implicit val codec: JsCodec[Username] =
      summonCodec[String](JsonCodecMaker.make).refine[NonEmpty].asNewtype[Username]
    implicit val schema: JsSchema[Username] =
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
    implicit val schema: JsSchema[Password] =
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
    implicit val schema: JsSchema[PaginationOffset] =
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
    implicit val schema: JsSchema[PaginationLimit] =
      summonSchema[Int Refined Positive].asNewtype[PaginationLimit]
  }

  @newtype final case class PaginationHasNext(bool: Boolean)
  object PaginationHasNext {
    def unapply(hasNext: PaginationHasNext): Option[Boolean] = hasNext.bool.some

    implicit val codec: JsCodec[PaginationHasNext] =
      summonCodec[Boolean](JsonCodecMaker.make).asNewtype[PaginationHasNext]
    implicit val schema: JsSchema[PaginationHasNext] =
      summonSchema[Boolean].asNewtype[PaginationHasNext]
  }
}
