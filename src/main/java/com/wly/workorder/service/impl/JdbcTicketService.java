package com.wly.workorder.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wly.workorder.auth.AuthContext;
import com.wly.workorder.auth.AuthRole;
import com.wly.workorder.auth.AuthSession;
import com.wly.workorder.common.PageResult;
import com.wly.workorder.model.TicketModels.Attachment;
import com.wly.workorder.model.TicketModels.CreateFeedbackRequest;
import com.wly.workorder.model.TicketModels.Feedback;
import com.wly.workorder.model.TicketModels.FeedbackReply;
import com.wly.workorder.model.TicketModels.ReplyFeedbackRequest;
import com.wly.workorder.model.TicketModels.TicketStatus;
import com.wly.workorder.model.TicketModels.UpdateWorkOrderStatusRequest;
import com.wly.workorder.model.TicketModels.WorkOrder;
import com.wly.workorder.model.TicketModels.WorkOrderSummary;
import com.wly.workorder.service.TicketService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JdbcTicketService implements TicketService {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public JdbcTicketService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public PageResult<Feedback> pageFeedback(String keyword, TicketStatus status, int pageNum, int pageSize) {
    AuthSession session = AuthContext.require();
    List<Feedback> items = queryFeedbacks(session, keyword, status);
    return page(items, pageNum, pageSize);
  }

  @Override
  public Feedback getFeedbackById(String id) {
    AuthSession session = AuthContext.require();
    List<Feedback> items = queryFeedbacks(session, null, null).stream()
      .filter(item -> Objects.equals(item.getId(), id))
      .collect(Collectors.toList());
    return items.isEmpty() ? null : items.get(0);
  }

  @Override
  @Transactional
  public Feedback createFeedback(CreateFeedbackRequest request) {
    AuthSession session = AuthContext.require();
    String id = "fb-" + UUID.randomUUID().toString().substring(0, 8);
    String code = nextCode("FB-");
    String now = now();
    String imagesJson = writeJson(request.getImages() == null ? List.of() : request.getImages());
    String attachmentsJson = writeJson(request.getAttachments() == null ? List.of() : request.getAttachments());

    jdbcTemplate.update(
      "insert into wo_feedback (id, code, title, description, status, owner_username, account_name, assignee, images_json, attachments_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      id, code, request.getTitle(), request.getDescription(), TicketStatus.PENDING.name(), session.getUsername(),
      defaultString(request.getAccountName(), session.getDisplayName()), "", imagesJson, attachmentsJson, now, now
    );

    return getFeedbackById(id);
  }

  @Override
  @Transactional
  public Feedback replyFeedback(String id, ReplyFeedbackRequest request) {
    Feedback feedback = getFeedbackById(id);
    if (feedback == null) {
      return null;
    }

    AuthSession session = AuthContext.require();
    String replyRole = session.getRole() == AuthRole.ADMIN ? "service" : "user";
    String replyAuthor = defaultString(request.getAuthor(), session.getDisplayName());
    String now = now();
    jdbcTemplate.update(
      "insert into wo_feedback_reply (id, feedback_id, role, author, content, created_at) values (?, ?, ?, ?, ?, ?)",
      "rep-" + UUID.randomUUID().toString().substring(0, 8),
      id,
      replyRole,
      replyAuthor,
      request.getContent(),
      now
    );

    if (session.getRole() == AuthRole.ADMIN) {
      jdbcTemplate.update(
        "update wo_feedback set assignee = case when assignee is null or assignee = '' then ? else assignee end where id = ?",
        session.getDisplayName(),
        id
      );
    }

    if (feedback.getStatus() == TicketStatus.PENDING) {
      jdbcTemplate.update(
        "update wo_feedback set status = ?, updated_at = ? where id = ?",
        TicketStatus.PROCESSING.name(), now, id
      );
    } else {
      jdbcTemplate.update("update wo_feedback set updated_at = ? where id = ?", now, id);
    }

    return getFeedbackById(id);
  }

  @Override
  public PageResult<WorkOrder> pageWorkOrders(String keyword, TicketStatus status, int pageNum, int pageSize) {
    AuthSession session = AuthContext.require();
    if (session.getRole() != AuthRole.ADMIN) {
      throw new IllegalStateException("Only admin can access work orders");
    }
    List<WorkOrder> items = queryFeedbacks(session, keyword, status).stream()
      .map(this::mapWorkOrderFromFeedback)
      .collect(Collectors.toList());
    return page(items, pageNum, pageSize);
  }

  @Override
  public WorkOrderSummary getWorkOrderSummary(String keyword, TicketStatus status) {
    AuthSession session = AuthContext.require();
    if (session.getRole() != AuthRole.ADMIN) {
      throw new IllegalStateException("Only admin can access work order summary");
    }

    List<Feedback> items = queryFeedbacks(session, keyword, status);
    long pending = items.stream().filter(item -> item.getStatus() == TicketStatus.PENDING).count();
    long processing = items.stream().filter(item -> item.getStatus() == TicketStatus.PROCESSING).count();
    long solved = items.stream().filter(item -> item.getStatus() == TicketStatus.SOLVED).count();
    long closed = items.stream().filter(item -> item.getStatus() == TicketStatus.CLOSED).count();
    return WorkOrderSummary.builder()
      .total(items.size())
      .pending(pending)
      .processing(processing)
      .solved(solved)
      .closed(closed)
      .build();
  }

  @Override
  @Transactional
  public WorkOrder updateWorkOrderStatus(String id, UpdateWorkOrderStatusRequest request) {
    AuthSession session = AuthContext.require();
    if (session.getRole() != AuthRole.ADMIN) {
      throw new IllegalStateException("Only admin can update work orders");
    }

    String now = now();
    jdbcTemplate.update(
      "update wo_feedback set assignee = case when assignee is null or assignee = '' then ? else assignee end where id = ?",
      session.getDisplayName(),
      id
    );
    int updated = jdbcTemplate.update(
      "update wo_feedback set status = ?, updated_at = ? where id = ?",
      request.getStatus().name(), now, id
    );
    if (updated == 0) {
      return null;
    }

    return queryWorkOrderById(id);
  }

  private List<Feedback> queryFeedbacks(AuthSession session, String keyword, TicketStatus status) {
    StringBuilder sql = new StringBuilder(
      "select id, code, title, description, status, owner_username, account_name, assignee, images_json, attachments_json, created_at, updated_at " +
        "from wo_feedback where 1=1"
    );
    List<Object> args = new ArrayList<>();
    if (session.getRole() != AuthRole.ADMIN) {
      sql.append(" and owner_username = ?");
      args.add(session.getUsername());
    }
    if (status != null) {
      sql.append(" and status = ?");
      args.add(status.name());
    }
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and (lower(code) like ? or lower(title) like ? or lower(description) like ? or lower(account_name) like ? or lower(assignee) like ?)");
      String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    sql.append(" order by updated_at desc");

    List<Feedback> feedbacks = jdbcTemplate.query(sql.toString(), this::mapFeedback, args.toArray());
    if (feedbacks.isEmpty()) {
      return feedbacks;
    }

    Map<String, List<FeedbackReply>> repliesByFeedbackId = queryRepliesByFeedbackIds(
      feedbacks.stream().map(Feedback::getId).collect(Collectors.toList())
    );
    for (Feedback feedback : feedbacks) {
      feedback.setReplies(repliesByFeedbackId.getOrDefault(feedback.getId(), List.of()));
    }
    return feedbacks;
  }

  private Map<String, List<FeedbackReply>> queryRepliesByFeedbackIds(List<String> feedbackIds) {
    if (feedbackIds.isEmpty()) {
      return Map.of();
    }
    String placeholders = feedbackIds.stream().map(item -> "?").collect(Collectors.joining(","));
    String sql = "select id, feedback_id, role, author, content, created_at from wo_feedback_reply where feedback_id in (" + placeholders + ") order by created_at asc";
    List<FeedbackReplyRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new FeedbackReplyRow(
      rs.getString("id"),
      rs.getString("feedback_id"),
      rs.getString("role"),
      rs.getString("author"),
      rs.getString("content"),
      rs.getString("created_at")
    ), feedbackIds.toArray());
    Map<String, List<FeedbackReply>> grouped = new LinkedHashMap<>();
    for (FeedbackReplyRow row : rows) {
      grouped.computeIfAbsent(row.feedbackId, key -> new ArrayList<>()).add(FeedbackReply.builder()
        .id(row.id)
        .role(row.role)
        .author(row.author)
        .content(row.content)
        .createdAt(row.createdAt)
        .build());
    }
    return grouped;
  }

  private List<WorkOrder> queryWorkOrders(String keyword, TicketStatus status) {
    AuthSession session = AuthContext.require();
    return queryFeedbacks(session, keyword, status).stream()
      .map(this::mapWorkOrderFromFeedback)
      .collect(Collectors.toList());
  }

  private WorkOrder queryWorkOrderById(String id) {
    List<WorkOrder> items = jdbcTemplate.query(
      "select id, code, title, description, status, owner_username, account_name, assignee, images_json, attachments_json, created_at, updated_at from wo_feedback where id = ?",
      (rs, rowNum) -> mapWorkOrderFromFeedback(mapFeedback(rs, rowNum)),
      id
    );
    return items.isEmpty() ? null : items.get(0);
  }

  private Feedback mapFeedback(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return Feedback.builder()
      .id(rs.getString("id"))
      .code(rs.getString("code"))
      .title(rs.getString("title"))
      .description(rs.getString("description"))
      .status(TicketStatus.valueOf(rs.getString("status")))
      .ownerUsername(rs.getString("owner_username"))
      .accountName(rs.getString("account_name"))
      .assignee(rs.getString("assignee"))
      .createdAt(rs.getString("created_at"))
      .updatedAt(rs.getString("updated_at"))
      .images(readStringList(rs.getString("images_json")))
      .attachments(readAttachments(rs.getString("attachments_json")))
      .replies(List.of())
      .build();
  }

  private WorkOrder mapWorkOrder(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return WorkOrder.builder()
      .id(rs.getString("id"))
      .code(rs.getString("code"))
      .title(rs.getString("title"))
      .description(rs.getString("description"))
      .status(TicketStatus.valueOf(rs.getString("status")))
      .assignee(rs.getString("assignee"))
      .createdAt(rs.getString("created_at"))
      .updatedAt(rs.getString("updated_at"))
      .build();
  }

  private WorkOrder mapWorkOrderFromFeedback(Feedback feedback) {
    return WorkOrder.builder()
      .id(feedback.getId())
      .code(feedback.getCode())
      .title(feedback.getTitle())
      .description(feedback.getDescription())
      .status(feedback.getStatus())
      .assignee(defaultString(feedback.getAssignee(), "未分配"))
      .createdAt(feedback.getCreatedAt())
      .updatedAt(feedback.getUpdatedAt())
      .build();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize json", ex);
    }
  }

  private List<String> readStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  private List<Attachment> readAttachments(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Attachment>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  private String nextCode(String prefix) {
    Integer max = jdbcTemplate.queryForObject(
      "select coalesce(max(cast(substring(code, 4) as signed)), 0) from wo_feedback where code like ?",
      Integer.class,
      prefix + "%"
    );
    if (max == null) {
      max = 0;
    }
    return prefix + String.format("%03d", max + 1);
  }

  private static String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String now() {
    return LocalDateTime.now().format(FMT);
  }

  private static <T> PageResult<T> page(List<T> list, int pageNum, int pageSize) {
    int safePageNum = Math.max(pageNum, 1);
    int safePageSize = Math.max(pageSize, 1);
    int fromIndex = Math.min((safePageNum - 1) * safePageSize, list.size());
    int toIndex = Math.min(fromIndex + safePageSize, list.size());
    return PageResult.of(list.size(), safePageNum, safePageSize, list.subList(fromIndex, toIndex));
  }

  private record FeedbackReplyRow(
    String id,
    String feedbackId,
    String role,
    String author,
    String content,
    String createdAt
  ) {
  }
}
