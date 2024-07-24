package io.branchtalk.discussions

import cats.effect.IO
import io.branchtalk.discussions.model._
import io.branchtalk.shared.model.{ ID, UUID.Generator }
import io.branchtalk.shared.Fixtures._

trait DiscussionsFixtures {

  def editorIDCreate(implicit uuidGenerator: UUID.Generator): IO[ID[User]] = ID.create[IO, User]

  def subscriberIDCreate(implicit uuidGenerator: UUID.Generator): IO[ID[User]] = ID.create[IO, User]

  def voterIDCreate(implicit uuidGenerator: UUID.Generator): IO[ID[User]] = ID.create[IO, User]

  def channelCreate(implicit uuidGenerator: UUID.Generator): IO[Channel.Create] =
    (
      ID.create[IO, User],
      noWhitespaces.flatMap(Channel.UrlName.parse[IO]),
      nameLike.flatMap(Channel.Name.parse[IO]),
      textProducer.map(_.loremIpsum).flatMap(Channel.Description.parse[IO]).map(Option.apply)
    ).mapN(Channel.Create.apply)

  def postCreate(channelID: ID[Channel])(implicit uuidGenerator: UUID.Generator): IO[Post.Create] =
    (
      ID.create[IO, User],
      channelID.pure[IO],
      nameLike.flatMap(Post.Title.parse[IO]),
      textProducer.map(_.loremIpsum).map(Post.Text(_)).map(Post.Content.Text(_))
    ).mapN(Post.Create.apply)

  def commentCreate(postID: ID[Post])(implicit uuidGenerator: UUID.Generator): IO[Comment.Create] =
    (
      ID.create[IO, User],
      postID.pure[IO],
      textProducer.map(_.loremIpsum).map(Comment.Content(_)),
      none[ID[Comment]].pure[IO]
    ).mapN(Comment.Create.apply)
}
