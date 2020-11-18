package io.branchtalk.users.infrastructure

import cats.Id
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.{ ID, UUID }
import io.branchtalk.users.model.{ Password, Permission, Permissions, Session }
import io.estatico.newtype.Coercible
import org.postgresql.util.PGobject

object DoobieExtensions {

  implicit val passwordAlgorithmMeta: Meta[Password.Algorithm] =
    pgEnumString("password_algorithm", Password.Algorithm.withNameInsensitive, _.entryName.toLowerCase)

  implicit val sessionUsageTypeMeta: Meta[Session.Usage.Type] =
    pgEnumString("session_usage_type", Session.Usage.Type.withNameInsensitive, _.entryName.toLowerCase)

  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit val permissionsMeta: Meta[Permissions] = {
    implicit def idCodec[A](implicit ev: Coercible[UUID, ID[A]]): JsonValueCodec[ID[A]] =
      Coercible.unsafeWrapMM[JsonValueCodec, Id, UUID, ID[A]].apply(JsonCodecMaker.make)
    implicit val permissionCodec: JsonValueCodec[Permission] = JsonCodecMaker.make[Permission]
    implicit val permissionsCodec: JsonValueCodec[Permissions] =
      Coercible[JsonValueCodec[Set[Permission]], JsonValueCodec[Permissions]].apply(JsonCodecMaker.make)

    val jsonType = "jsonb"

    // imap instead of timap because a @newtype cannot have TypeTag
    Meta.Advanced.other[PGobject](jsonType).imap[Permissions](pgObj => readFromString[Permissions](pgObj.getValue)) {
      permissions => new PGobject().tap(_.setType(jsonType)).tap(_.setValue(writeToString(permissions)))
    }
  }
}
