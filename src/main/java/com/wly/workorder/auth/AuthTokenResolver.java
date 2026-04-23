package com.wly.workorder.auth;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class AuthTokenResolver {
  private AuthTokenResolver() {
  }

  public static String resolve() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (!(attrs instanceof ServletRequestAttributes servletRequestAttributes)) {
      return "";
    }
    String header = servletRequestAttributes.getRequest().getHeader("Authorization");
    if (header == null || header.isBlank()) {
      return "";
    }
    if (header.startsWith("Bearer ")) {
      return header.substring(7).trim();
    }
    return header.trim();
  }
}
