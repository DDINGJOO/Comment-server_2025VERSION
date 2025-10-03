package com.teambind.commentserver.web;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiErrorResponse {
  private final Instant timestamp;
  private final int status;
  private final String code;
  private final String message;
  private final String path;

  public static ApiErrorResponse of(int status, String code, String message, String path) {
    return ApiErrorResponse.builder()
        .timestamp(Instant.now())
        .status(status)
        .code(code)
        .message(message)
        .path(path)
        .build();
  }
}
