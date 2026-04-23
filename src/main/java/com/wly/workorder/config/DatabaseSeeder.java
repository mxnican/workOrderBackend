package com.wly.workorder.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wly.workorder.model.TicketModels.TicketStatus;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public DatabaseSeeder(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void run(String... args) throws Exception {
    ensureAssigneeColumn();
    ensureAvatarColumn();

    Integer userCount = jdbcTemplate.queryForObject("select count(*) from wo_user", Integer.class);
    if (userCount != null && userCount > 0) {
      return;
    }

    String now = now();
    jdbcTemplate.update(
      "insert into wo_user (id, username, password, display_name, avatar_url, role, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
      "user-1", "user", "user123", "普通用户", "", "USER", now, now
    );
    jdbcTemplate.update(
      "insert into wo_user (id, username, password, display_name, avatar_url, role, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
      "admin-1", "admin", "admin123", "系统管理员", "", "ADMIN", now, now
    );

    jdbcTemplate.update(
      "insert into wo_feedback (id, code, title, description, status, owner_username, account_name, assignee, images_json, attachments_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      "fb-1", "FB-001", "导出任务在大数据量下失败",
      "每次导出超过 10 万条数据时都会超时失败，影响了月度报表生成，请尽快协助排查。",
      TicketStatus.PROCESSING.name(), "user", "西湖托育中心", "客服一组",
      json(List.of(textAsset("附件-导出日志.txt", "导出日志示例：导出任务在 60% 左右超时"))),
      json(List.of("data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='112' viewBox='0 0 160 112'%3E%3Crect width='160' height='112' rx='18' fill='%237aa7ff'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='white' font-size='16'%3E导出报错%3C/text%3E%3C/svg%3E")),
      now, now
    );
    jdbcTemplate.update(
      "insert into wo_feedback (id, code, title, description, status, owner_username, account_name, assignee, images_json, attachments_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      "fb-2", "FB-002", "移动端导航在小屏幕重叠",
      "手机横屏进入系统后，侧边导航和顶部导航会发生重叠，部分按钮无法点击。",
      TicketStatus.CLOSED.name(), "user", "城南幼儿园", "前端组", "[]", "[]", now, now
    );
    jdbcTemplate.update(
      "insert into wo_feedback (id, code, title, description, status, owner_username, account_name, assignee, images_json, attachments_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      "fb-3", "FB-003", "账号权限配置后仍看不到模块",
      "账号已经分配了查看权限，但登录后依旧无法进入数据模块，请协助检查权限配置。",
      TicketStatus.SOLVED.name(), "user", "余杭护理院", "权限组",
      "[]",
      json(List.of(textAsset("权限说明.docx", "权限说明：已补齐角色菜单授权和数据看板查看范围"))),
      now, now
    );
    jdbcTemplate.update(
      "insert into wo_feedback (id, code, title, description, status, owner_username, account_name, assignee, images_json, attachments_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      "fb-4", "FB-004", "打印预览按钮点击无响应",
      "申请单详情页里的打印预览按钮点了没有任何反应，怀疑是浏览器兼容问题。",
      TicketStatus.PENDING.name(), "user", "滨江园区", "", "[]", "[]", now, now
    );

    jdbcTemplate.update(
      "insert into wo_feedback_reply (id, feedback_id, role, author, content, created_at) values (?, ?, ?, ?, ?, ?)",
      "rep-1", "fb-1", "user", "西湖托育中心", "<p>导出到 60% 左右时会提示超时。</p>", now
    );
    jdbcTemplate.update(
      "insert into wo_feedback_reply (id, feedback_id, role, author, content, created_at) values (?, ?, ?, ?, ?, ?)",
      "rep-2", "fb-1", "service", "系统管理员", "<p>已收到反馈，正在定位导出任务的超时原因。</p>", now
    );
    jdbcTemplate.update(
      "insert into wo_feedback_reply (id, feedback_id, role, author, content, created_at) values (?, ?, ?, ?, ?, ?)",
      "rep-3", "fb-2", "service", "系统管理员", "<p>该问题已在新版本中修复，请刷新页面后再试。</p>", now
    );
    jdbcTemplate.update(
      "insert into wo_feedback_reply (id, feedback_id, role, author, content, created_at) values (?, ?, ?, ?, ?, ?)",
      "rep-4", "fb-3", "service", "系统管理员", "<p>已重新校准角色菜单权限，问题已处理完成。</p>", now
    );
    jdbcTemplate.update(
      "insert into wo_feedback_reply (id, feedback_id, role, author, content, created_at) values (?, ?, ?, ?, ?, ?)",
      "rep-5", "fb-3", "user", "余杭护理院", "<p>现在可以正常查看了，感谢处理。</p>", now
    );
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private Map<String, Object> textAsset(String name, String text) {
    return Map.of(
      "uid", "att-" + name.hashCode(),
      "name", name,
      "url", "data:text/plain;charset=utf-8," + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8),
      "fileUrl", "data:text/plain;charset=utf-8," + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8),
      "serverPath", "data:text/plain;charset=utf-8," + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8),
      "ext", "txt"
    );
  }

  private void ensureAssigneeColumn() {
    jdbcTemplate.execute((Connection connection) -> {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, "wo_feedback", "assignee")) {
        if (columns.next()) {
          return null;
        }
      }
      try (Statement statement = connection.createStatement()) {
        statement.execute("alter table wo_feedback add column assignee varchar(128) not null default ''");
      }
      return null;
    });
  }

  private void ensureAvatarColumn() {
    jdbcTemplate.execute((Connection connection) -> {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, "wo_user", "avatar_url")) {
        if (columns.next()) {
          return null;
        }
      }
      try (Statement statement = connection.createStatement()) {
        statement.execute("alter table wo_user add column avatar_url text");
      }
      return null;
    });
  }

  private static String now() {
    return LocalDateTime.now().format(FMT);
  }
}
