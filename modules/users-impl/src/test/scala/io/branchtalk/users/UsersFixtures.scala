package io.branchtalk.users

import cats.effect.{ Clock, IO }
import io.branchtalk.shared.model.*
import io.branchtalk.users.model.{ Ban, Channel, Password, Session, User }
import io.branchtalk.shared.Fixtures.*

import scala.util.Random

trait UsersFixtures {

  def channelIDCreate(using UUID.Generator): IO[ID[Channel]] =
    ID.create[IO, Channel]

  def passwordCreate(password: String = "pass"): IO[Password] =
    ParseNewtype[IO].parse[Password.Raw](password.getBytes).map(Password.create)

  def userCreate: IO[User.Create] =
    (
      company().map(_.getEmail).map(e => s"${Random.nextLong()}+$e").flatMap(ParseNewtype[IO].parse[User.Email](_)),
      textProducer.map(_.randomString(10)).flatMap(ParseNewtype[IO].parse[User.Name](_)),
      textProducer.map(_.loremIpsum()).flatMap(ParseNewtype[IO].parse[User.Description](_)).map(_.some),
      passwordCreate()
    ).mapN(User.Create.apply)

  def sessionCreate(userID: ID[User])(using Clock[IO]): IO[Session.Create] =
    (
      userID.pure[IO],
      (Session.Usage.UserSession: Session.Usage).pure[IO],
      Session.ExpirationTime.now[IO]
    ).mapN(Session.Create.apply)

  def banCreate(userID: ID[User], channelID: ID[Channel]): IO[Ban] =
    (
      userID.pure[IO],
      textProducer.map(_.loremIpsum()).flatMap(ParseNewtype[IO].parse[Ban.Reason](_)),
      Ban.Scope.ForChannel(channelID).pure[IO]
    ).mapN(Ban.apply _)
}
