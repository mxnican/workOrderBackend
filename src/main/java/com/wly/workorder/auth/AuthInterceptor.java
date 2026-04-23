package com.wly.workorder.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wly.workorder.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
  private final DefaultAuthService authService;
  private final ObjectMapper objectMapper;

  public AuthInterceptor(DefaultAuthService authService, ObjectMapper objectMapper) {
    this.authService = authService;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String path = request.getRequestURI();
    if (path.equals("/api/health") || path.equals("/api/auth/login") || path.equals("/api/auth/me") || path.startsWith("/api/auth/demo-account/") || path.startsWith("/actuator")) {
      return true;
    }
    if (path.startsWith("/api/files/") && "GET".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    try {
      AuthSession session = authService.requireSession();
      if (path.startsWith("/api/work-order") && session.getRole() != AuthRole.ADMIN) {
        writeError(response, ApiResponse.withCode(403, "forbidden", null));
        return false;
      }
      if (path.startsWith("/api/feedback") && session.getRole() != AuthRole.USER && session.getRole() != AuthRole.ADMIN) {
        writeError(response, ApiResponse.withCode(403, "forbidden", null));
        return false;
      }
      AuthContext.set(session);
      return true;
    } catch (AuthException ex) {
      writeError(response, ex.getResponse());
      return false;
    }
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    AuthContext.clear();
  }

  private void writeError(HttpServletResponse response, ApiResponse<?> body) throws Exception {
    response.setStatus(200);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), body);
  }
}
