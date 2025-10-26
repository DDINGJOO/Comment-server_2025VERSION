package com.teambind.commentserver.event.events;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 생성 이벤트 (불변 객체)
 *
 * <p>사용자가 게시글에 첫 댓글을 작성했을 때 발행되는 도메인 이벤트입니다.
 * 2일 윈도우 내에서 동일 사용자의 첫 댓글에 대해서만 발행됩니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreatedEvent implements DomainEvent {
  private String writerId;
  private String articleId;
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Override
  public Instant occurredAt() {
    return createdAt;
  }
}
