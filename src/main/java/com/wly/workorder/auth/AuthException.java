package com.wly.workorder.auth;

import com.wly.workorder.common.ApiResponse;

public class AuthException extends RuntimeException {
  private final ApiResponse<?> response;

  public AuthException(ApiResponse<?> response) {
    super(response.getMsg());
    this.response = response;
  }

  public ApiResponse<?> getResponse() {
    return response;
  }
}
