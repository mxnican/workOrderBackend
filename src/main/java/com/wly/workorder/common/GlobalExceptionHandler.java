package com.wly.workorder.common;

import com.wly.workorder.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AuthException.class)
  public ApiResponse<?> handleAuthException(AuthException ex) {
    return ex.getResponse();
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<?> handleException(Exception ex) {
    return ApiResponse.withCode(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), null);
  }
}
