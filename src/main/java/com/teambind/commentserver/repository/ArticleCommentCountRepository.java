package com.teambind.commentserver.repository;

import com.teambind.commentserver.entity.ArticleCommentCount;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ArticleCommentCountRepository extends CrudRepository<ArticleCommentCount, String> {

  Optional<ArticleCommentCount> findByArticleId(String articleId);

  /**
   * article_comment_counts 테이블에서 카운트를 원자적으로 증가시킵니다. 필요 시 INSERT ... ON DUPLICATE KEY 형태로 먼저 레코드가
   * 없으면 생성하는 로직은 서비스에 구현하세요.
   */
  @Modifying
  @Transactional
  @Query(
      value =
          "UPDATE article_comment_counts SET comment_count = comment_count + 1 WHERE article_id = :articleId",
      nativeQuery = true)
  int incrementCommentCount(@Param("articleId") String articleId);

  /** 카운트를 원자적으로 감소합니다. 0 미만이 되지 않도록 DB 쿼리에서 제한합니다. */
  @Modifying
  @Transactional
  @Query(
      value =
          "UPDATE article_comment_counts SET comment_count = GREATEST(comment_count - 1, 0) WHERE article_id = :articleId",
      nativeQuery = true)
  int decrementCommentCount(@Param("articleId") String articleId);

  /**
   * article_comment_counts에 레코드가 없을 때 새로 삽입하거나 기존 값에 delta를 더하는 안전한 upsert 방식. MariaDB/MySQL 의 ON
   * DUPLICATE KEY UPDATE 를 사용합니다. delta는 음수일 수 있으므로 입력값 검증 필요.
   */
  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO article_comment_counts (article_id, comment_count) VALUES (:articleId, :delta) "
              + "ON DUPLICATE KEY UPDATE comment_count = GREATEST(comment_count + :delta, 0)",
      nativeQuery = true)
  int upsertAndAdd(@Param("articleId") String articleId, @Param("delta") int delta);

  /** 카운트를 명시적으로 세팅합니다 (보정용). */
  @Modifying
  @Transactional
  @Query(
      value =
          "UPDATE article_comment_counts SET comment_count = :count WHERE article_id = :articleId",
      nativeQuery = true)
  int setCount(@Param("articleId") String articleId, @Param("count") int count);
}
