package io.branchtalk.discussions

import cats.effect.IO
import io.branchtalk.discussions.model.*
import io.branchtalk.shared.model.*
import io.branchtalk.shared.Fixtures.*

trait DiscussionsFixtures {

  def editorIDCreate(using UUID.Generator): IO[ID[User]] = ID.create[IO, User]

  def subscriberIDCreate(using UUID.Generator): IO[ID[User]] = ID.create[IO, User]

  def voterIDCreate(using UUID.Generator): IO[ID[User]] = ID.create[IO, User]

  def channelCreate(using UUID.Generator): IO[Channel.Create] =
    (
      ID.create[IO, User],
      noWhitespaces.flatMap(ParseNewtype[IO].parse[Channel.UrlName](_)),
      nameLike.flatMap(ParseNewtype[IO].parse[Channel.Name](_)),
      textProducer.map(_.loremIpsum).flatMap(ParseNewtype[IO].parse[Channel.Description](_)).map(Option.apply)
    ).mapN(Channel.Create.apply)

  def postCreate(channelID: ID[Channel])(using UUID.Generator): IO[Post.Create] =
    (
      ID.create[IO, User],
      channelID.pure[IO],
      nameLike.flatMap(ParseNewtype[IO].parse[Post.Title](_)),
      textProducer.map(_.loremIpsum).map(Post.Text(_)).map(Post.Content.Text(_))
    ).mapN(Post.Create.apply)

  def commentCreate(postID: ID[Post])(using UUID.Generator): IO[Comment.Create] =
    (
      ID.create[IO, User],
      postID.pure[IO],
      textProducer.map(_.loremIpsum).map(Comment.Content(_)),
      none[ID[Comment]].pure[IO]
    ).mapN(Comment.Create.apply)
}
