package io.branchtalk.users.email

import cats.effect.Sync
import io.branchtalk.logging.Logger
import io.branchtalk.users.EmailService
import io.branchtalk.users.model.User

final class LoggingEmailService[F[_]: Sync](logger: Logger[F]) extends EmailService[F] {

  override def sendEmailConfirmation(
    recipientEmail: User.Email,
    token:          User.EmailConfirmationToken
  ): F[Unit] =
    logger.info(
      show"[EMAIL-STUB] Confirmation email to=${recipientEmail.show} token=${token.show} — no SMTP configured, token returned in API response for testing"
    )
}
