package io.branchtalk.users.infrastructure

import cats.Id
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import io.branchtalk.shared.infrastructure.DoobieSupport._
import io.branchtalk.shared.model.{ ID, UUID, branchtalkLocale }
import io.branchtalk.users.model.{ Ban, Password, Permission, Permissions, Session }
import io.estatico.newtype.Coercible
import org.postgresql.util.PGobject

import scala.annotation.nowarn

object DoobieExtensions {

  implicit val banScopeTypeMeta: Meta[Ban.Scope.Type] =
    pgEnumString("user_ban_type", Ban.Scope.Type.withNameInsensitive, _.entryName.toLowerCase(branchtalkLocale))

  implicit val passwordAlgorithmMeta: Meta[Password.Algorithm] =
    pgEnumString("password_algorithm",
                 Password.Algorithm.withNameInsensitive,
                 _.entryName.toLowerCase(branchtalkLocale)
    )

  implicit val sessionUsageTypeMeta: Meta[Session.Usage.Type] =
    pgEnumString("session_usage_type",
                 Session.Usage.Type.withNameInsensitive,
                 _.entryName.toLowerCase(branchtalkLocale)
    )

  @nowarn("cat=unused") // macros
  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit private def idCodec[A](implicit ev: Coercible[UUID, ID[A]]): JsonValueCodec[ID[A]] =
    Coercible.unsafeWrapMM[JsonValueCodec, Id, UUID, ID[A]].apply(JsonCodecMaker.make)
  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit private val permissionCodec: JsonValueCodec[Permission] = JsonCodecMaker.make[Permission]
  @SuppressWarnings(Array("org.wartremover.warts.All")) // macros
  implicit private val permissionsCodec: JsonValueCodec[Permissions] =
    Coercible[JsonValueCodec[Set[Permission]], JsonValueCodec[Permissions]].apply(JsonCodecMaker.make)

  private val jsonType = "jsonb"

  implicit val permissionMeta: Meta[Permission] =
    Meta.Advanced.other[PGobject](jsonType).timap[Permission](pgObj => readFromString[Permission](pgObj.getValue)) {
      permission => new PGobject().tap(_.setType(jsonType)).tap(_.setValue(writeToString(permission)))
    }

  implicit val permissionsMeta: Meta[Permissions] =
    // imap instead of timap because a @newtype cannot have TypeTag
    Meta.Advanced.other[PGobject](jsonType).imap[Permissions](pgObj => readFromString[Permissions](pgObj.getValue)) {
      permissions => new PGobject().tap(_.setType(jsonType)).tap(_.setValue(writeToString(permissions)))
    }
}
