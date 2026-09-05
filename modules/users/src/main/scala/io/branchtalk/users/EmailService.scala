package io.branchtalk.users

import io.branchtalk.users.model.User

trait EmailService[F[_]] {

  def sendEmailConfirmation(
    recipientEmail: User.Email,
    token:          User.EmailConfirmationToken
  ): F[Unit]
}
