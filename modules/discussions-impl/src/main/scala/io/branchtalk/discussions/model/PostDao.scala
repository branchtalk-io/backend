package io.branchtalk.discussions.model

import io.branchtalk.shared.model.{ CreationTime, ID, ModificationTime }
import io.scalaland.chimney.dsl._

final case class PostDao(
  id:                 ID[Post],
  authorID:           ID[User],
  channelID:          ID[Channel],
  urlTitle:           Post.UrlTitle,
  title:              Post.Title,
  contentType:        Post.Content.Type,
  contentRaw:         Post.Content.Raw,
  createdAt:          CreationTime,
  lastModifiedAt:     Option[ModificationTime],
  commentsNr:         Post.CommentsNr,
  upvotes:            Post.Upvotes,
  downvores:          Post.Downvotes,
  totalScore:         Post.TotalScore,
  controversialScore: Post.ControversialScore
) {

  def toDomain: Post =
    Post(id = id,
         data = this.into[Post.Data].withFieldConst(_.content, Post.Content.Tupled(contentType, contentRaw)).transform
    )
}
object PostDao {

  def fromDomain(post: Post): PostDao = {
    val Post.Content.Tupled(contentType, contentRaw) = post.data.content
    post.data
      .into[PostDao]
      .withFieldConst(_.id, post.id)
      .withFieldConst(_.contentType, contentType)
      .withFieldConst(_.contentRaw, contentRaw)
      .transform
  }
}
