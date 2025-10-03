package com.teambind.commentserver.exceptions;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
  private final ErrorCode errorcode;

  public CustomException(ErrorCode errorcode) {
    super(errorcode.getMessage()); // 에러 메시지는 한글 메시지로 노출
    this.errorcode = errorcode;
  }

  public HttpStatus getStatus() {
    return errorcode.getStatus();
  }

  public ErrorCode getErrorcode() {
    return errorcode;
  }
}
