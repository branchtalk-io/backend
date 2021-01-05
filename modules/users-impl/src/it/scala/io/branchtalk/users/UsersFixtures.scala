package io.branchtalk.users

import cats.effect.{ Clock, IO }
import io.branchtalk.shared.model._
import io.branchtalk.users.model.{ Ban, Channel, Password, Session, User }
import io.branchtalk.shared.Fixtures._

trait UsersFixtures {

  def channelIDCreate(implicit uuidGenerator: UUIDGenerator): IO[ID[Channel]] =
    ID.create[IO, Channel]

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

  def banCreate(userID: ID[User], channelID: ID[Channel]): IO[Ban] =
    (
      userID.pure[IO],
      textProducer.map(_.loremIpsum()).flatMap(Ban.Reason.parse[IO]),
      Ban.Scope.ForChannel(channelID).pure[IO]
    ).mapN(Ban.apply _)
}
