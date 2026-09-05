package io.branchtalk.users.email

import cats.effect.{ IO, Ref }
import io.branchtalk.users.EmailService
import io.branchtalk.users.model.User

final class TestEmailService(ref: Ref[IO, List[TestEmailService.SentEmail]]) extends EmailService[IO] {

  override def sendEmailConfirmation(
    recipientEmail: User.Email,
    token:          User.EmailConfirmationToken
  ): IO[Unit] =
    ref.update(TestEmailService.SentEmail(recipientEmail, token) :: _)

  def sentEmails: IO[List[TestEmailService.SentEmail]] = ref.get
}
object TestEmailService {

  final case class SentEmail(
    to:    User.Email,
    token: User.EmailConfirmationToken
  )

  def create: IO[TestEmailService] =
    Ref.of[IO, List[SentEmail]](List.empty).map(new TestEmailService(_))
}
