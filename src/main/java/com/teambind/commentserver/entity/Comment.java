package com.teambind.commentserver.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import lombok.*;

/**
 * 댓글 엔티티
 *
 * <p>이 엔티티는 도메인 주도 설계 원칙을 따릅니다: - 불변 필드(ID, articleId, writerId 등)는 생성 후 변경 불가 - 상태 변경은
 * 도메인 메서드(updateContents, markDeleted 등)를 통해서만 수행 - 비즈니스 로직은 엔티티 내부에 캡슐화
 *
 * <p>주의: setter는 JPA와 테스트를 위해 존재하지만, 프로덕션 코드에서는 도메인 메서드만 사용해야 합니다.
 */
@Entity
@Table(
    name = "comments",
    indexes = {
      @Index(name = "idx_comment_article_created", columnList = "article_id, created_at"),
      @Index(name = "idx_comment_parent", columnList = "parent_comment_id"),
      @Index(name = "idx_comment_root", columnList = "root_comment_id"),
      @Index(name = "idx_comment_writer", columnList = "writer_id"),
      @Index(name = "idx_comment_status", columnList = "status"),
      // 페이지네이션 쿼리 최적화를 위한 복합 인덱스
      @Index(
          name = "idx_comment_article_depth_status",
          columnList = "article_id, depth, is_deleted, status"),
      // 게시글의 활성 댓글 카운트 쿼리 최적화
      @Index(
          name = "idx_comment_article_status_deleted",
          columnList = "article_id, status, is_deleted")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

  // 불변 필드: 생성 후 변경하지 말 것 (setter는 JPA/테스트용)
  @Id
  @Column(name = "comment_id", length = 100, nullable = false)
  private String commentId;

  @Column(name = "article_id", length = 100, nullable = false)
  private String articleId; // 외부 Article 서비스 id

  @Column(name = "writer_id", length = 100, nullable = false)
  private String writerId; // 외부 User 서비스 id

  @Column(name = "parent_comment_id", length = 100)
  private String parentCommentId; // 부모 댓글 id (null이면 최상위)

  @Column(name = "root_comment_id", length = 100)
  private String rootCommentId; // 스레드 루트 id (self 또는 최상위 id)

  @Column(name = "depth", nullable = false)
  @Builder.Default
  private Integer depth = 0; // 0: 루트, 1: 1뎁스, 2: 2뎁스

  // 변경 가능한 필드: 도메인 메서드를 통해서만 변경할 것
  @Column(name = "contents", columnDefinition = "TEXT", nullable = false)
  private String contents;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private Boolean isDeleted = Boolean.FALSE;

  @Column(name = "status", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private CommentStatus status = CommentStatus.ACTIVE; // ACTIVE, HIDDEN, BANNED, PENDING_REVIEW 등

  @Column(name = "reply_count", nullable = false)
  @Builder.Default
  private Integer replyCount = 0;

  // 시간 필드: JPA 라이프사이클 콜백에서 관리
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  // 정적 팩토리 메서드

  /**
   * 루트 댓글을 생성하는 팩토리 메서드
   *
   * @param commentId 댓글 ID (PrimaryKeyProvider로 생성된 값)
   * @param articleId 게시글 ID
   * @param writerId 작성자 ID
   * @param contents 댓글 내용
   * @return 생성된 루트 댓글 엔티티
   * @throws IllegalArgumentException contents가 null이거나 blank인 경우
   */
  public static Comment createRoot(
      String commentId, String articleId, String writerId, String contents) {
    Objects.requireNonNull(commentId, "commentId must not be null");
    Objects.requireNonNull(articleId, "articleId must not be null");
    Objects.requireNonNull(writerId, "writerId must not be null");
    validateContents(contents);

    return Comment.builder()
        .commentId(commentId)
        .articleId(articleId)
        .writerId(writerId)
        .contents(contents)
        .depth(0)
        .rootCommentId(commentId)
        .parentCommentId(null)
        .build();
  }

  /**
   * 답글(대댓글)을 생성하는 팩토리 메서드
   *
   * @param commentId 댓글 ID (PrimaryKeyProvider로 생성된 값)
   * @param parent 부모 댓글
   * @param writerId 작성자 ID
   * @param contents 댓글 내용
   * @return 생성된 답글 엔티티
   * @throws IllegalArgumentException parent가 null이거나 contents가 null/blank인 경우
   */
  public static Comment createReply(
      String commentId, Comment parent, String writerId, String contents) {
    Objects.requireNonNull(commentId, "commentId must not be null");
    Objects.requireNonNull(parent, "parent comment must not be null");
    Objects.requireNonNull(writerId, "writerId must not be null");
    validateContents(contents);

    Integer parentDepth = parent.getDepth() != null ? parent.getDepth() : 0;
    String rootId =
        parent.getRootCommentId() != null
            ? parent.getRootCommentId()
            : parent.getCommentId();

    return Comment.builder()
        .commentId(commentId)
        .articleId(parent.getArticleId())
        .writerId(writerId)
        .contents(contents)
        .depth(parentDepth + 1)
        .rootCommentId(rootId)
        .parentCommentId(parent.getCommentId())
        .build();
  }

  /**
   * 댓글 내용 유효성 검증
   *
   * @param contents 검증할 내용
   * @throws IllegalArgumentException contents가 null이거나 blank인 경우
   */
  private static void validateContents(String contents) {
    if (contents == null || contents.isBlank()) {
      throw new IllegalArgumentException("contents must not be null or blank");
    }
  }

  // 비즈니스 편의 메서드들

  /**
   * 답글이 추가될 때 호출하는 연관관계 편의 메서드 (도메인 메서드) 부모 댓글의 replyCount를 1 증가시킨다.
   */
  public void addReply() {
    this.setReplyCount((this.replyCount == null) ? 1 : this.replyCount + 1);
  }

  /**
   * 답글이 제거될 때 호출하는 연관관계 편의 메서드 (도메인 메서드) 부모 댓글의 replyCount를 1 감소시킨다. 0 미만으로 내려가지 않도록
   * 방어한다.
   */
  public void removeReply() {
    if (this.replyCount == null || this.replyCount <= 0) {
      this.setReplyCount(0);
    } else {
      this.setReplyCount(this.replyCount - 1);
    }
  }

  /**
   * @deprecated Use {@link #addReply()} instead
   */
  @Deprecated
  public void incrementReplyCount() {
    addReply();
  }

  /**
   * @deprecated Use {@link #removeReply()} instead
   */
  @Deprecated
  public void decrementReplyCount() {
    removeReply();
  }

  /**
   * 댓글 내용을 수정한다. (도메인 메서드 - 프로덕션 코드에서 사용)
   *
   * @param newContents 새로운 댓글 내용
   * @throws IllegalArgumentException newContents가 null이거나 blank인 경우
   */
  public void updateContents(String newContents) {
    validateContents(newContents);
    this.setContents(newContents);
  }

  /**
   * 주어진 사용자 ID가 이 댓글의 작성자인지 확인한다.
   *
   * @param writerId 확인할 사용자 ID
   * @return 작성자가 맞으면 true, 아니면 false
   */
  public boolean isOwnedBy(String writerId) {
    return Objects.equals(this.writerId, writerId);
  }

  /**
   * 댓글을 삭제 상태로 변경한다. (도메인 메서드)
   */
  public void markDeleted() {
    this.setIsDeleted(Boolean.TRUE);
    this.setDeletedAt(Instant.now());
    this.setStatus(CommentStatus.DELETED);
  }

  /**
   * 댓글을 숨김 상태로 변경한다. (도메인 메서드)
   */
  public void markHidden() {
    this.setStatus(CommentStatus.HIDDEN);
  }

  /**
   * 댓글이 수정되었는지 여부를 반환한다.
   *
   * @return 수정되었으면 true, 아니면 false
   */
  public boolean isEdited() {
    return this.updatedAt != null && !this.updatedAt.equals(this.createdAt);
  }

  /**
   * 사용자에게 표시할 댓글 내용을 반환한다. (도메인 로직)
   *
   * <p>댓글의 상태(삭제, 숨김, 제재 등)에 따라 적절한 메시지를 반환한다.
   *
   * @return 표시할 댓글 내용
   */
  public String getDisplayContents() {
    // 삭제된 댓글인 경우
    if (Boolean.TRUE.equals(this.isDeleted)) {
      return "삭제된 댓글입니다.";
    }

    // 상태에 따라 다른 메시지 반환
    return switch (this.status) {
      case ACTIVE -> this.contents;
      case HIDDEN -> "숨김 처리된 댓글입니다.";
      case BANNED -> "제재된 댓글입니다.";
      case PENDING_REVIEW -> "검토 중인 댓글입니다.";
      case DELETED -> "삭제된 댓글입니다.";
    };
  }

  /**
   * 댓글이 사용자에게 보여야 하는지 여부를 반환한다.
   *
   * <p>현재 정책: 모든 댓글은 트리 구조 유지를 위해 visible=true로 표시
   * (내용은 getDisplayContents()로 마스킹됨)
   *
   * @return 항상 true (트리 구조 유지)
   */
  public boolean isVisibleInTree() {
    return true; // 트리 구조 유지를 위해 모든 댓글 노출 (내용은 마스킹됨)
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
