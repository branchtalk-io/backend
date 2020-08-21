package io.branchtalk.discussions

import cats.effect.IO
import io.branchtalk.discussions.model._
import io.branchtalk.shared.models.{ ID, UUIDGenerator }
import io.branchtalk.shared.Fixtures._

trait DiscussionsFixtures {

  def channelCreate(implicit uuidGenerator: UUIDGenerator): IO[Channel.Create] =
    (
      ID.create[IO, User],
      noWhitespaces.flatMap(Channel.UrlName.parse[IO]),
      nameLike.flatMap(Channel.Name.parse[IO]),
      textProducer.map(_.loremIpsum).flatMap(Channel.Description.parse[IO]).map(Option.apply)
    ).mapN(Channel.Create.apply)
}
