package com.wly.workorder.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class TicketModels {
  private TicketModels() {
  }

  public enum TicketStatus {
    PENDING,
    PROCESSING,
    SOLVED,
    CLOSED
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Attachment {
    private String uid;
    private String name;
    private String url;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FeedbackReply {
    private String id;
    private String role;
    private String author;
    private String content;
    private String createdAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Feedback {
    private String id;
    private String code;
    private String title;
    private String description;
    private TicketStatus status;
    private String ownerUsername;
    private String accountName;
    private String assignee;
    private String createdAt;
    private String updatedAt;
    private List<String> images;
    private List<Attachment> attachments;
    private List<FeedbackReply> replies;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WorkOrder {
    private String id;
    private String code;
    private String title;
    private String description;
    private TicketStatus status;
    private String assignee;
    private String createdAt;
    private String updatedAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WorkOrderSummary {
    private long total;
    private long pending;
    private long processing;
    private long solved;
    private long closed;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateFeedbackRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private String accountName;
    private List<String> images;
    private List<Attachment> attachments;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReplyFeedbackRequest {
    @NotBlank
    private String content;

    private String author;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateWorkOrderStatusRequest {
    @NotNull
    private TicketStatus status;

    private String remark;
  }
}
