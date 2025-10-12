package com.teambind.commentserver.event.publish;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDeletedEvent {
  private String writerId;
  private String articleId;
  private LocalDateTime createdAt;
}
