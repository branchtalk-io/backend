package io.branchtalk.users.email

import cats.effect.Sync
import io.branchtalk.configs.SmtpConfig
import io.branchtalk.logging.Logger
import io.branchtalk.users.EmailService
import io.branchtalk.users.model.User
import jakarta.mail.*
import jakarta.mail.internet.{ InternetAddress, MimeMessage }

import java.util.Properties

final class SmtpEmailService[F[_]: Sync](config: SmtpConfig, logger: Logger[F]) extends EmailService[F] {

  private val props: Properties = {
    val p = new Properties()
    p.put("mail.smtp.host", config.host)
    p.put("mail.smtp.port", config.port.toString)
    p.put("mail.smtp.auth", config.auth.toString)
    p.put("mail.smtp.starttls.enable", config.startTls.toString)
    p
  }

  private val authenticator: Option[Authenticator] =
    (config.username, config.password).mapN { (user, pass) =>
      new Authenticator {
        override def getPasswordAuthentication: PasswordAuthentication = new PasswordAuthentication(user, pass)
      }
    }

  override def sendEmailConfirmation(
    recipientEmail: User.Email,
    token:          User.EmailConfirmationToken
  ): F[Unit] =
    Sync[F].blocking {
      val session = authenticator.fold(Session.getInstance(props))(Session.getInstance(props, _))
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(config.from))
      message.setRecipients(Message.RecipientType.TO, recipientEmail.unwrap)
      message.setSubject("Branchtalk — Confirm your email address")
      message.setText(
        s"""Please confirm your email address by using this token:
           |
           |  ${token.unwrap}
           |
           |Use this token with the email confirmation endpoint to complete the change.
           |""".stripMargin
      )
      Transport.send(message)
    } >> logger.info(show"Confirmation email sent to ${recipientEmail.show} via SMTP ${config.host}:${config.port}")
}
