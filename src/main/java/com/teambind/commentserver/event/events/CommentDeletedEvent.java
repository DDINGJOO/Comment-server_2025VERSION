package com.teambind.commentserver.event.events;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 삭제 이벤트 (불변 객체)
 *
 * <p>게시글의 마지막 댓글이 삭제되었을 때 발행되는 도메인 이벤트입니다.
 * 남은 활성 댓글 수가 0일 때만 발행됩니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDeletedEvent implements DomainEvent {
  private String writerId;
  private String articleId;
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Override
  public Instant occurredAt() {
    return createdAt;
  }
}
