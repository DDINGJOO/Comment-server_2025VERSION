package com.teambind.commentserver.dto;

import com.teambind.commentserver.entity.Comment;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CommentResponse {
  private String commentId;
  private String articleId;
  private String writerId;
  private String parentCommentId;
  private String rootCommentId;
  private Integer depth;
  private String contents;
  private Boolean isDeleted;
  private Comment.CommentStatus status;
  private Integer replyCount;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;

  public static CommentResponse from(Comment c) {
    return CommentResponse.builder()
        .commentId(c.getCommentId())
        .articleId(c.getArticleId())
        .writerId(c.getWriterId())
        .parentCommentId(c.getParentCommentId())
        .rootCommentId(c.getRootCommentId())
        .depth(c.getDepth())
        .contents(c.getContents())
        .isDeleted(c.getIsDeleted())
        .status(c.getStatus())
        .replyCount(c.getReplyCount())
        .createdAt(c.getCreatedAt())
        .updatedAt(c.getUpdatedAt())
        .deletedAt(c.getDeletedAt())
        .build();
  }
}
