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

  // 편의 메서드
  public void increment() {
    if (this.commentCount == null) this.commentCount = 0;
    this.commentCount = this.commentCount + 1;
  }

  public void decrement() {
    if (this.commentCount == null || this.commentCount <= 0) {
      this.commentCount = 0;
    } else {
      this.commentCount = this.commentCount - 1;
    }
  }

  public void setCount(int count) {
    this.commentCount = Math.max(0, count);
  }
}
