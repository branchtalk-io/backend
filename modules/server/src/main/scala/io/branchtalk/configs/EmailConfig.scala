package io.branchtalk.configs

import io.branchtalk.shared.infrastructure.PureconfigSupport.{ *, given }
import io.branchtalk.shared.model.ShowPretty

final case class SmtpConfig(
  host:     String,
  port:     Int,
  username: Option[String],
  password: Option[String],
  startTls: Boolean = true,
  auth:     Boolean = true,
  from:     String
) derives ConfigReader,
      ShowPretty

enum EmailConfig derives ConfigReader, ShowPretty {
  case Smtp(smtp: SmtpConfig)
  case Log
}
