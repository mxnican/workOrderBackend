package com.wly.workorder.auth;

import com.wly.workorder.common.ApiResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthService implements AuthService {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final JdbcTemplate jdbcTemplate;
  private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

  public DefaultAuthService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public LoginResponse login(LoginRequest request) {
    AuthUser user = jdbcTemplate.query(
      "select username, password, display_name, avatar_url, role from wo_user where username = ?",
      rs -> rs.next()
        ? new AuthUser(
          rs.getString("username"),
          rs.getString("password"),
          rs.getString("display_name"),
          rs.getString("avatar_url"),
          AuthRole.valueOf(rs.getString("role"))
        )
        : null,
      request.getUsername()
    );
    if (user == null || !user.getPassword().equals(request.getPassword())) {
      throw new AuthException(ApiResponse.withCode(401, "invalid username or password", null));
    }
    AuthSession session = new AuthSession(UUID.randomUUID().toString(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl(), user.getRole());
    sessions.put(session.getToken(), session);
    return new LoginResponse(session.getToken(), toProfile(session));
  }

  @Override
  public UserProfile me() {
    AuthSession session = requireSession();
    return toProfile(session);
  }

  @Override
  public DemoAccountResponse getDemoAccount(String username) {
    String normalized = normalizeDemoUsername(username);
    if (normalized == null) {
      throw new AuthException(ApiResponse.withCode(400, "unsupported demo account", null));
    }
    AuthUser user = jdbcTemplate.query(
      "select username, password, display_name, avatar_url, role from wo_user where username = ?",
      rs -> rs.next()
        ? new AuthUser(
          rs.getString("username"),
          rs.getString("password"),
          rs.getString("display_name"),
          rs.getString("avatar_url"),
          AuthRole.valueOf(rs.getString("role"))
        )
        : null,
      normalized
    );
    if (user == null) {
      throw new AuthException(ApiResponse.withCode(404, "demo account not found", null));
    }
    return new DemoAccountResponse(user.getUsername(), user.getPassword());
  }

  @Override
  public UserProfile updateProfile(UpdateProfileRequest request) {
    AuthSession session = requireSession();
    String avatarUrl = request.getAvatarUrl() == null ? "" : request.getAvatarUrl().trim();
    jdbcTemplate.update(
      "update wo_user set display_name = ?, avatar_url = ?, updated_at = ? where username = ?",
      request.getDisplayName().trim(),
      avatarUrl,
      now(),
      session.getUsername()
    );
    updateSessions(session.getUsername(), updatedSession -> {
      updatedSession.setDisplayName(request.getDisplayName().trim());
      updatedSession.setAvatarUrl(avatarUrl);
    });
    return me();
  }

  @Override
  public UserProfile updatePassword(UpdatePasswordRequest request) {
    AuthSession session = requireSession();
    AuthUser user = jdbcTemplate.query(
      "select username, password, display_name, avatar_url, role from wo_user where username = ?",
      rs -> rs.next()
        ? new AuthUser(
          rs.getString("username"),
          rs.getString("password"),
          rs.getString("display_name"),
          rs.getString("avatar_url"),
          AuthRole.valueOf(rs.getString("role"))
        )
        : null,
      session.getUsername()
    );
    if (user == null || !user.getPassword().equals(request.getOldPassword())) {
      throw new AuthException(ApiResponse.withCode(400, "old password is incorrect", null));
    }
    jdbcTemplate.update(
      "update wo_user set password = ?, updated_at = ? where username = ?",
      request.getNewPassword(),
      now(),
      session.getUsername()
    );
    return me();
  }

  @Override
  public AuthSession requireSession() {
    String token = AuthTokenResolver.resolve();
    AuthSession session = sessions.get(token);
    if (session == null) {
      throw new AuthException(ApiResponse.withCode(401, "unauthorized", null));
    }
    return session;
  }

  public void logout(String token) {
    sessions.remove(token);
  }

  private UserProfile toProfile(AuthSession session) {
    return new UserProfile(session.getUsername(), session.getDisplayName(), session.getAvatarUrl(), session.getRole());
  }

  private void updateSessions(String username, java.util.function.Consumer<AuthSession> consumer) {
    sessions.values().stream()
      .filter(session -> username.equals(session.getUsername()))
      .forEach(consumer);
  }

  private String normalizeDemoUsername(String username) {
    String normalized = String.valueOf(username == null ? "" : username).trim().toLowerCase();
    if ("user".equals(normalized) || "admin".equals(normalized)) {
      return normalized;
    }
    return null;
  }

  private static String now() {
    return LocalDateTime.now().format(FMT);
  }
}
