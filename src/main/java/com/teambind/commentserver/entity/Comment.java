package com.teambind.commentserver.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
    name = "comment",
    indexes = {
      @Index(name = "idx_comment_article_created", columnList = "article_id, created_at"),
      @Index(name = "idx_comment_parent", columnList = "parent_comment_id"),
      @Index(name = "idx_comment_root", columnList = "root_comment_id"),
      @Index(name = "idx_comment_writer", columnList = "writer_id"),
      @Index(name = "idx_comment_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

  @Id
  @Column(name = "comment_id", length = 100, nullable = false)
  private String commentId; // UUID 권장

  @Column(name = "article_id", length = 100, nullable = false)
  private String articleId; // 외부 Article 서비스 id

  @Column(name = "writer_id", length = 100, nullable = false)
  private String writerId; // 외부 User 서비스 id

  @Column(name = "parent_comment_id", length = 100)
  private String parentCommentId; // 부모 댓글 id (null이면 최상위)

  @Column(name = "root_comment_id", length = 100)
  private String rootCommentId; // 스레드 루트 id (self 또는 최상위 id)

  @Column(name = "depth", nullable = false)
  private Integer depth = 0; // 0: 루트, 1: 1뎁스, 2: 2뎁스

  @Column(name = "contents", columnDefinition = "TEXT", nullable = false)
  private String contents;

  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = Boolean.FALSE;

  @Column(name = "status", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private CommentStatus status = CommentStatus.ACTIVE; // ACTIVE, HIDDEN, BANNED, PENDING_REVIEW 등

  @Column(name = "reply_count", nullable = false)
  private Integer replyCount = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  // 비즈니스 편의 메서드들

  public void incrementReplyCount() {
    this.replyCount = (this.replyCount == null) ? 1 : this.replyCount + 1;
  }

  public void decrementReplyCount() {
    if (this.replyCount == null || this.replyCount <= 0) {
      this.replyCount = 0;
    } else {
      this.replyCount = this.replyCount - 1;
    }
  }

  public void markDeleted() {
    this.isDeleted = Boolean.TRUE;
    this.deletedAt = Instant.now();
    this.status = CommentStatus.DELETED;
  }

  public void markHidden() {
    this.status = CommentStatus.HIDDEN;
  }

  @PrePersist
  public void prePersist() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.depth == null) {
      this.depth = 0;
    }
    if (this.replyCount == null) {
      this.replyCount = 0;
    }
    if (this.isDeleted == null) {
      this.isDeleted = Boolean.FALSE;
    }
    if (this.status == null) {
      this.status = CommentStatus.ACTIVE;
    }
    // rootCommentId 기본 설정: 루트(부모가 없으면 self) 처리는 서비스 레이어에서 commentId 생성 후 설정 권장
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }

  public enum CommentStatus {
    ACTIVE, // 공개
    HIDDEN, // 신고/검토로 숨김(관리자/심사 후 복구 가능)
    BANNED, // 정책 위반으로 영구 비공개(또는 차단)
    PENDING_REVIEW, // 자동필터 등에 의해 검토중
    DELETED; // 작성자/관리자에 의한 soft-delete 표기

    public boolean isVisibleToUsers() {
      return this == ACTIVE;
    }

    public boolean isModerationRequired() {
      return this == PENDING_REVIEW;
    }
  }
}
