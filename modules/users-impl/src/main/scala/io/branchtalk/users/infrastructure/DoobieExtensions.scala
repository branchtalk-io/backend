package io.branchtalk.users.infrastructure

import cats.Id
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.*
import io.branchtalk.users.model.*
import org.postgresql.util.PGobject

import scala.annotation.nowarn
import scala.collection.compat.immutable.ArraySeq

object DoobieExtensions {

  given banScopeTypeMeta: Meta[Ban.Scope.Type] = pgEnumString(
    "user_ban_type",
    name =>
      Ban.Scope.Type.values
        .find(_.entryName.equalsIgnoreCase(name))
        .getOrElse(
          throw new NoSuchElementException(s"$name is not a member of Enum (${Ban.Scope.Type.values.mkString(", ")})")
        ),
    _.entryName.toLowerCase(branchtalkLocale)
  )

  given passwordAlgorithmMeta: Meta[Password.Algorithm] = pgEnumString(
    "password_algorithm",
    Password.Algorithm.withNameInsensitive,
    _.entryName.toLowerCase(branchtalkLocale)
  )

  given passwordHashMeta: Meta[Password.Hash] = Password.Hash.unsafeMakeF(Meta.apply)

  given passwordSaltMeta: Meta[Password.Salt] = Password.Salt.unsafeMakeF(Meta.apply)

  given sessionUsageTypeMeta: Meta[Session.Usage.Type] = pgEnumString(
    "session_usage_type",
    name =>
      Session.Usage.Type.values
        .find(_.entryName.equalsIgnoreCase(name))
        .getOrElse(
          throw new NoSuchElementException(
            s"$name is not a member of Enum (${Session.Usage.Type.values.mkString(", ")})"
          )
        ),
    _.entryName.toLowerCase(branchtalkLocale)
  )

  given sessionExpirationTime: Meta[Session.ExpirationTime] = Session.ExpirationTime.unsafeMakeF(Meta.apply)

  given sensitiveDataAlgorithmTypeMeta: Meta[SensitiveData.Algorithm] =
    pgEnumString(
      "data_encryption_algorithm",
      name =>
        SensitiveData.Algorithm.values
          .find(_.entryName.equalsIgnoreCase(name))
          .getOrElse(
            throw new NoSuchElementException(
              s"$name is not a member of Enum (${SensitiveData.Algorithm.values.mkString(", ")})"
            )
          ),
      _.entryName.toLowerCase(branchtalkLocale)
    )

  given sensitiveDataKey: Meta[SensitiveData.Key] =
    SensitiveData.Key.unsafeMakeF[Meta](Meta[Array[Byte]].timap[ArraySeq[Byte]](ArraySeq.from(_))(a => a.toArray))

  private given JsonValueCodec[Permission] = {
    given [A]: JsonValueCodec[ID[A]] = ID.unsafeMakeF[JsonValueCodec, A](JsonCodecMaker.make[UUID])
    JsonCodecMaker.make[Permission]
  }
  private given JsonValueCodec[Permissions] =
    Permissions.unsafeMakeF[JsonValueCodec](JsonCodecMaker.make[Set[Permission]])

  private val jsonType = "jsonb"

  given permissionMeta: Meta[Permission] =
    Meta.Advanced.other[PGobject](jsonType).timap[Permission](pgObj => readFromString[Permission](pgObj.getValue)) {
      permission => new PGobject().tap(_.setType(jsonType)).tap(_.setValue(writeToString(permission)))
    }

  given permissionsMeta: Meta[Permissions] =
    // imap instead of timap because a @newtype cannot have TypeTag
    Meta.Advanced.other[PGobject](jsonType).imap[Permissions](pgObj => readFromString[Permissions](pgObj.getValue)) {
      permissions => new PGobject().tap(_.setType(jsonType)).tap(_.setValue(writeToString(permissions)))
    }
}
