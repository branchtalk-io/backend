package io.branchtalk.users

import cats.effect.{ Clock, IO }
import io.branchtalk.shared.models._
import io.branchtalk.users.model.{ Password, Session, User }
import io.branchtalk.shared.Fixtures._

trait UsersFixtures {

  def passwordCreate(password: String = "pass"): IO[Password] =
    Password.Raw.parse[IO](password.getBytes).map(Password.create)

  def userCreate: IO[User.Create] =
    (
      company().map(_.getEmail).flatMap(User.Email.parse[IO]),
      textProducer.map(_.randomString(10)).flatMap(User.Name.parse[IO]),
      textProducer.map(_.loremIpsum()).map(User.Description(_).some),
      passwordCreate()
    ).mapN(User.Create.apply)

  def sessionCreate(userID: ID[User])(implicit clock: Clock[IO]): IO[Session.Create] =
    (
      userID.pure[IO],
      (Session.Usage.UserSession: Session.Usage).pure[IO],
      Session.ExpirationTime.now[IO]
    ).mapN(Session.Create.apply)
}
