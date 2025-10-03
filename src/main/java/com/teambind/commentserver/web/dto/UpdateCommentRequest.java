package com.teambind.commentserver.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCommentRequest {
  @NotBlank(message = "contents는 비어 있을 수 없습니다.")
  private String contents;
}
