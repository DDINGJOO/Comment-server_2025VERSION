package com.teambind.commentserver.repository;

import com.teambind.commentserver.entity.Comment;
import com.teambind.commentserver.entity.Comment.CommentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

  List<Comment> findByParentCommentIdOrderByCreatedAtAsc(String parentCommentId);

  List<Comment> findByArticleIdOrderByCreatedAtAsc(String articleId);

  List<Comment> findByRootCommentIdOrderByCreatedAtAsc(String rootCommentId);

  @Query(
      value =
          """
        WITH roots AS (
          SELECT comment_id, reply_count, created_at, (reply_count + 1) AS size_for_page
          FROM comments
          WHERE article_id = :articleId AND depth = 0 AND status = 'ACTIVE'
          ORDER BY created_at DESC
        ),
        cum AS (
          SELECT comment_id, created_at,
                 SUM(size_for_page) OVER (ORDER BY created_at DESC) AS cum_sum
          FROM roots
        )
        SELECT comment_id
        FROM cum
        WHERE cum_sum > :prevLimit AND cum_sum <= :currLimit
        ORDER BY created_at DESC
        """,
      nativeQuery = true)
  List<String> findRootIdsForPage(
      @Param("articleId") String articleId,
      @Param("prevLimit") long prevLimit,
      @Param("currLimit") long currLimit);

  // 루트 및 해당 루트의 모든 자식들을 한 번에 가져오는 메서드
  @Query(
      "SELECT c FROM Comment c "
          + "WHERE c.articleId = :articleId "
          + "AND (c.commentId IN :rootIds OR c.rootCommentId IN :rootIds) "
          + "AND (c.isDeleted = true OR c.status = com.teambind.commentserver.entity.Comment.CommentStatus.ACTIVE) "
          + "ORDER BY COALESCE(c.rootCommentId, c.commentId), c.depth, c.createdAt")
  List<Comment> findRootsAndChildrenByRootIds(
      @Param("articleId") String articleId, @Param("rootIds") List<String> rootIds);

  long countByArticleIdAndWriterIdAndIsDeletedFalseAndStatus(
      String articleId, String writerId, CommentStatus status);

  long countByArticleIdAndIsDeletedFalseAndStatus(String articleId, CommentStatus status);
}
