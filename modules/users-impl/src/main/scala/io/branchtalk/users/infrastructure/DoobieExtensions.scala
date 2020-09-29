package io.branchtalk.users.infrastructure

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.models.{ ID, UUID }
import io.branchtalk.users.model.{ Password, Permission, Permissions, Session }
import org.postgresql.util.PGobject

object DoobieExtensions {

  implicit val passwordAlgorithmMeta: Meta[Password.Algorithm] =
    pgEnumString("password_algorithm", Password.Algorithm.withNameInsensitive, _.entryName.toLowerCase)

  implicit val sessionUsageTypeMeta: Meta[Session.Usage.Type] =
    pgEnumString("session_usage_type", Session.Usage.Type.withNameInsensitive, _.entryName.toLowerCase)

  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit val permissionsMeta: Meta[Permissions] = {
    implicit def idCodec[A]: JsonValueCodec[ID[A]] =
      JsonCodecMaker.make[UUID].asInstanceOf[JsonValueCodec[ID[A]]]
    implicit val permissionCodec: JsonValueCodec[Permission] = JsonCodecMaker.make[Permission]
    implicit val permissionsCodec: JsonValueCodec[Permissions] =
      JsonCodecMaker.make[Set[Permission]].asInstanceOf[JsonValueCodec[Permissions]]

    val jsonType = "jsonb"

    // imap instead of timap because a @newtype cannot have TypeTag
    Meta.Advanced.other[PGobject](jsonType).imap[Permissions](pgObj => readFromString[Permissions](pgObj.getValue)) {
      permissions => new PGobject().tap(_.setType(jsonType)).tap(_.setValue(writeToString(permissions)))
    }
  }
}
