package io.branchtalk.discussions.reads

import cats.data.NonEmptyList
import cats.effect.Sync
import io.branchtalk.discussions.model.{ Comment, Post }
import io.branchtalk.shared.infrastructure.DoobieSupport.{ *, given }
import io.branchtalk.shared.model.{ ID, Paginated }

final class CommentReadsImpl[F[_]: Sync](transactor: Transactor[F]) extends CommentReads[F] {

  private val commonSelect: Fragment =
    fr"""SELECT id,
        |       author_id,
        |       channel_id,
        |       post_id,
        |       content,
        |       reply_to,
        |       nesting_level,
        |       created_at,
        |       last_modified_at,
        |       replies_nr,
        |       upvotes_nr,
        |       downvotes_nr,
        |       total_score,
        |       controversial_score
        |FROM comments""".stripMargin

  private val orderBy: Comment.Sorting => Fragment = {
    case Comment.Sorting.Newest        => fr"ORDER BY created_at DESC"
    case Comment.Sorting.Hottest       => fr"ORDER by total_score DESC"
    case Comment.Sorting.Controversial => fr"ORDER by controversial_score DESC"
  }

  private def idExists(id: ID[Comment]): Fragment = fr"id = $id AND deleted = FALSE"

  private def idDeleted(id: ID[Comment]): Fragment = fr"id = $id AND deleted = TRUE"

  override def paginate(
    post:      ID[Post],
    repliesTo: Option[ID[Comment]],
    sortBy:    Comment.Sorting,
    offset:    Paginated.Offset,
    limit:     Paginated.Limit
  ): F[Paginated[Comment]] =
    (commonSelect ++ Fragments.whereAndOpt(fr"post_id = $post".some,
                                           repliesTo.map(parent => fr"reply_to = $parent"),
                                           fr"deleted = FALSE".some
    ) ++ orderBy(sortBy))
      .paginate[Comment](offset,
                         limit,
                         show"Paginate Discussions' Comment from $offset taking $limit sorted by $sortBy"
      )
      .transact(transactor)

  override def exists(id: ID[Comment]): F[Boolean] =
    (fr"SELECT 1 FROM comments WHERE" ++ idExists(id))
      .exists(show"Discussions' Comment ID=$id exists")
      .transact(transactor)

  override def deleted(id: ID[Comment]): F[Boolean] =
    (fr"SELECT 1 FROM comments WHERE" ++ idDeleted(id))
      .exists(show"Discussions' Comment ID=$id deleted")
      .transact(transactor)

  override def getById(id: ID[Comment], isDeleted: Boolean = false): F[Option[Comment]] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .queryWithLabel[Comment](show"Get Discussions' Comment by ID=$id")
      .option
      .transact(transactor)

  override def requireById(id: ID[Comment], isDeleted: Boolean = false): F[Comment] =
    (commonSelect ++ fr"WHERE" ++ (if (isDeleted) idDeleted(id) else idExists(id)))
      .queryWithLabel[Comment](show"Require Discussions' Comment by ID=$id")
      .failNotFound("Comment", id)
      .transact(transactor)

  override def search(
    query:  String,
    postID: Option[ID[Post]],
    offset: Paginated.Offset,
    limit:  Paginated.Limit
  ): F[Paginated[Comment]] = {
    val requiredFilters = NonEmptyList.of(
      fr"search_vector @@ plainto_tsquery('english', $query)",
      fr"deleted = FALSE"
    )
    val filters = postID.fold(requiredFilters)(pid => requiredFilters :+ fr"post_id = $pid")
    (commonSelect ++ Fragments.whereAnd(filters)
      ++ fr"ORDER BY ts_rank(search_vector, plainto_tsquery('english', $query)) DESC")
      .paginate[Comment](offset, limit, show"Search Discussions' Comments for query from $offset taking $limit")
      .transact(transactor)
  }
}
