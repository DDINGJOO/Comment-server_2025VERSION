package com.teambind.commentserver.repository;

import com.teambind.commentserver.entity.Comment;
import com.teambind.commentserver.entity.Comment.CommentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Comment 리포지토리
 *
 * <p>성능 최적화 포인트:
 * - 복합 인덱스를 활용한 쿼리 최적화
 * - N+1 문제 방지를 위한 한 번에 조회 (findRootsAndChildrenByRootIds)
 * - Window function을 활용한 효율적인 페이지네이션
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

  /**
   * 부모 댓글의 답글 목록 조회
   *
   * <p>인덱스 활용: idx_comment_parent
   */
  List<Comment> findByParentCommentIdOrderByCreatedAtAsc(String parentCommentId);

  /**
   * 게시글의 삭제되지 않은 댓글 목록 조회
   *
   * <p>인덱스 활용: idx_comment_article_status_deleted
   */
  List<Comment> findByArticleIdAndIsDeletedFalseOrderByCreatedAtAsc(String articleId);

  /**
   * 루트 댓글의 전체 스레드 조회
   *
   * <p>인덱스 활용: idx_comment_root
   */
  List<Comment> findByRootCommentIdOrderByCreatedAtAsc(String rootCommentId);

  /**
   * 페이지네이션을 위한 루트 댓글 ID 조회 (성능 최적화됨)
   *
   * <p>복합 인덱스 활용: idx_comment_article_created (article_id, created_at)
   * Window function으로 누적 합산하여 페이지 범위 계산
   */
  @Query(
      value =
          """
        WITH roots AS (
          SELECT comment_id, reply_count, created_at, (reply_count + 1) AS size_for_page
          FROM comments
          WHERE article_id = :articleId
            AND depth = 0
            AND is_deleted = false
            AND status = 'ACTIVE'
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

  /**
   * 루트 댓글 및 해당 루트의 모든 자식들을 한 번에 조회 (N+1 문제 방지)
   *
   * <p>인덱스 활용: idx_comment_article_status_deleted, idx_comment_root
   * 성능: 단일 쿼리로 루트 + 모든 자식 조회
   */
  @Query(
      "SELECT c FROM Comment c "
          + "WHERE c.articleId = :articleId "
          + "AND (c.commentId IN :rootIds OR c.rootCommentId IN :rootIds) "
          + "AND c.isDeleted = false "
          + "AND c.status = com.teambind.commentserver.entity.Comment.CommentStatus.ACTIVE "
          + "ORDER BY COALESCE(c.rootCommentId, c.commentId), c.depth, c.createdAt")
  List<Comment> findRootsAndChildrenByRootIds(
      @Param("articleId") String articleId, @Param("rootIds") List<String> rootIds);

  /**
   * 게시글에서 특정 사용자가 작성한 활성 댓글 수 조회
   *
   * <p>인덱스 활용: idx_comment_article_status_deleted
   * 용도: 첫 댓글 여부 판단 (현재 Redis로 대체됨)
   */
  long countByArticleIdAndWriterIdAndIsDeletedFalseAndStatus(
      String articleId, String writerId, CommentStatus status);

  /**
   * 게시글의 활성 댓글 수 조회
   *
   * <p>인덱스 활용: idx_comment_article_status_deleted
   * 용도: 마지막 댓글 삭제 이벤트 발행 여부 판단
   */
  long countByArticleIdAndIsDeletedFalseAndStatus(String articleId, CommentStatus status);
}
