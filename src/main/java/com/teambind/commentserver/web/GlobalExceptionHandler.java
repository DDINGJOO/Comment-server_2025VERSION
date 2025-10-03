package com.teambind.commentserver.web;

import com.teambind.commentserver.exceptions.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 게이트웨이에 전달할 수 있도록 에러 응답은 문자열 본문으로만 반환한다.
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<String> handleCustom(CustomException ex, HttpServletRequest req) {
    HttpStatus status = ex.getStatus();
    return ResponseEntity.status(status).body(ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<String> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    FieldError fe = ex.getBindingResult().getFieldError();
    String message =
        (fe != null && fe.getDefaultMessage() != null)
            ? fe.getDefaultMessage()
            : "요청 값이 올바르지 않습니다.";
    return ResponseEntity.status(status).body(message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<String> handleConstraint(
      ConstraintViolationException ex, HttpServletRequest req) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    String message = ex.getMessage() != null ? ex.getMessage() : "요청 값이 올바르지 않습니다.";
    return ResponseEntity.status(status).body(message);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleEtc(Exception ex, HttpServletRequest req) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    return new ResponseEntity<>("서버 내부 오류가 발생했습니다.", new HttpHeaders(), status);
  }
}
