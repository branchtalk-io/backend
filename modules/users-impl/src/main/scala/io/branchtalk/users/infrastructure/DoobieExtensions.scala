package io.branchtalk.users.infrastructure

import io.branchtalk.shared.infrastructure.DoobieSupport.{ pgEnumString, Meta }
import io.branchtalk.users.model.{ Password, Session }

object DoobieExtensions {

  implicit val passwordAlgorithmMeta: Meta[Password.Algorithm] =
    pgEnumString("password_algorithm", Password.Algorithm.withNameInsensitive, _.entryName.toLowerCase)

  implicit val sessionUsageTypeMeta: Meta[Session.Usage.Type] =
    pgEnumString("session_usage_type", Session.Usage.Type.withNameInsensitive, _.entryName.toLowerCase)

  // TODO: permissions Meta using Jsoniter codecs
}
