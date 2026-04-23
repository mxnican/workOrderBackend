package com.wly.workorder.auth;

public final class AuthContext {
  private static final ThreadLocal<AuthSession> CURRENT = new ThreadLocal<>();

  private AuthContext() {
  }

  public static void set(AuthSession session) {
    CURRENT.set(session);
  }

  public static AuthSession get() {
    return CURRENT.get();
  }

  public static AuthSession require() {
    AuthSession session = CURRENT.get();
    if (session == null) {
      throw new IllegalStateException("No authenticated user");
    }
    return session;
  }

  public static void clear() {
    CURRENT.remove();
  }
}
