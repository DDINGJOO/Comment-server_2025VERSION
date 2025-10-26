package com.teambind.commentserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 수정 요청 DTO (불변 객체)
 *
 * <p>불변성을 보장하여 스레드 안전성과 예측 가능한 동작을 제공합니다.
 * Jackson 역직렬화를 위해 NoArgsConstructor 제공, 테스트를 위해 Builder 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {
  @NotBlank(message = "writerId는 필수입니다.")
  private String writerId;

  @NotBlank(message = "contents는 비어 있을 수 없습니다.")
  private String contents;
}
