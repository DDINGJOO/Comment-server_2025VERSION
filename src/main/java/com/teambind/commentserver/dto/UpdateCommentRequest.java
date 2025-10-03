package com.teambind.commentserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCommentRequest {
  @NotBlank(message = "writerId는 필수입니다.")
  private String writerId;

  @NotBlank(message = "contents는 비어 있을 수 없습니다.")
  private String contents;
}
