package io.branchtalk.discussions.dao

import io.scalaland.catnip.Semi
import io.branchtalk.shared.models._

@Semi(FastEq, ShowPretty) final case class Post(
  id:   ID[Post],
  data: Post.Data
)
object Post extends PostProperties with PostCommands {

  @Semi(FastEq, ShowPretty) final case class Data(
    authorID:       ID[User],
    channelID:      ID[Channel],
    urlTitle:       Post.UrlTitle,
    title:          Post.Title,
    content:        Post.Content,
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  )
}
