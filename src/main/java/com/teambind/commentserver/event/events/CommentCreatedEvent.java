package com.teambind.commentserver.event.events;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreatedEvent {
  private String writerId;
  private String articleId;
  private LocalDateTime createdAt;
}
