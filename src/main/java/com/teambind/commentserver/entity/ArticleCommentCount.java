package com.teambind.commentserver.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * article_comment_counts 테이블 매핑 엔티티
 *
 * <p>DDL: CREATE TABLE article_comment_counts ( article_id VARCHAR(100) NOT NULL PRIMARY KEY,
 * comment_count INT NOT NULL DEFAULT 0, updated_at TIMESTAMP(6) NOT NULL DEFAULT
 * CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6), INDEX (comment_count) ) ENGINE=InnoDB
 * DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 */
@Entity
@Table(
    name = "article_comment_counts",
    indexes = {@Index(name = "idx_acc_comment_count", columnList = "comment_count")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCommentCount {

  @Id
  @Column(name = "article_id", length = 100, nullable = false)
  private String articleId;

  @Column(name = "comment_count", nullable = false)
  @Builder.Default
  private Integer commentCount = 0;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    if (this.commentCount == null) {
      this.commentCount = 0;
    }
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }

  /**
   * 댓글 수를 1 증가시킵니다. (도메인 메서드)
   */
  public void increment() {
    if (this.commentCount == null) {
      this.commentCount = 0;
    }
    this.commentCount = this.commentCount + 1;
  }

  /**
   * 댓글 수를 1 감소시킵니다. (도메인 메서드)
   *
   * <p>0 미만으로 내려가지 않도록 방어합니다.
   */
  public void decrement() {
    if (this.commentCount == null || this.commentCount <= 0) {
      this.commentCount = 0;
    } else {
      this.commentCount = this.commentCount - 1;
    }
  }

  /**
   * 댓글 수를 특정 값으로 설정합니다. (도메인 메서드)
   *
   * <p>주로 데이터 보정 용도로 사용됩니다.
   * 0 미만의 값은 0으로 조정됩니다.
   *
   * @param count 설정할 댓글 수
   */
  public void updateCount(int count) {
    this.commentCount = Math.max(0, count);
  }
}
