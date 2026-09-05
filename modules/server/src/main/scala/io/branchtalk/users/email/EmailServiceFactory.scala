package io.branchtalk.users.email

import cats.effect.Sync
import io.branchtalk.configs.EmailConfig
import io.branchtalk.logging.Logger
import io.branchtalk.users.EmailService

object EmailServiceFactory {

  def fromConfig[F[_]: Sync](config: EmailConfig, logger: Logger[F]): EmailService[F] = config match {
    case EmailConfig.Smtp(smtp) => SmtpEmailService[F](smtp, logger)
    case EmailConfig.Log        => LoggingEmailService[F](logger)
  }
}
