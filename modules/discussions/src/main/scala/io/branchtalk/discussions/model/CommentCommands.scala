package io.branchtalk.discussions.model

import io.scalaland.catnip.Semi
import io.branchtalk.shared.model.{ FastEq, ID, ShowPretty, Updatable }

object CommentCommands {

  @Semi(FastEq, ShowPretty) final case class Create(
    authorID: ID[User],
    postID:   ID[Post],
    content:  Comment.Content,
    replyTo:  Option[ID[Comment]]
  )

  @Semi(FastEq, ShowPretty) final case class Update(
    id:         ID[Comment],
    editorID:   ID[User],
    newContent: Updatable[Comment.Content]
  )

  @Semi(FastEq, ShowPretty) final case class Delete(
    id:       ID[Comment],
    editorID: ID[User]
  )

  @Semi(FastEq, ShowPretty) final case class Restore(
    id:       ID[Comment],
    editorID: ID[User]
  )

  @Semi(FastEq, ShowPretty) final case class Upvote(
    id:      ID[Comment],
    voterID: ID[User]
  )

  @Semi(FastEq, ShowPretty) final case class Downvote(
    id:      ID[Comment],
    voterID: ID[User]
  )

  @Semi(FastEq, ShowPretty) final case class RevokeVote(
    id:      ID[Comment],
    voterID: ID[User]
  )
}
