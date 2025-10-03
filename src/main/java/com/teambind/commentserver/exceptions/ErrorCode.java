package com.teambind.commentserver.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  // 댓글 관련 에러 코드 (메시지는 모두 한글)
  COMMENT_NOT_FOUND("CMT_404", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  PARENT_COMMENT_NOT_FOUND("CMT_404_P", "부모 댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  CONTENTS_REQUIRED("CMT_400", "댓글 내용은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
  NOT_COMMENT_OWNER("CMT_403", "작성자 본인만 댓글을 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN);

  private final String errCode;
  private final String message;
  private final HttpStatus status;

  ErrorCode(String errCode, String message, HttpStatus status) {
    this.status = status;
    this.errCode = errCode;
    this.message = message;
  }

  @Override
  public String toString() {
    return "ErrorCode{"
        + " status='"
        + status
        + '\''
        + ", errCode='"
        + errCode
        + '\''
        + ", message='"
        + message
        + '\''
        + '}';
  }
}
