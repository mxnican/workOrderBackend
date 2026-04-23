package com.wly.workorder.controller;

import com.wly.workorder.common.ApiResponse;
import com.wly.workorder.common.PageResult;
import com.wly.workorder.model.TicketModels.CreateFeedbackRequest;
import com.wly.workorder.model.TicketModels.Feedback;
import com.wly.workorder.model.TicketModels.ReplyFeedbackRequest;
import com.wly.workorder.model.TicketModels.TicketStatus;
import com.wly.workorder.model.TicketModels.UpdateWorkOrderStatusRequest;
import com.wly.workorder.model.TicketModels.WorkOrder;
import com.wly.workorder.model.TicketModels.WorkOrderSummary;
import com.wly.workorder.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TicketController {
  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @GetMapping("/health")
  public ApiResponse<String> health() {
    return ApiResponse.success("ok");
  }

  @GetMapping("/feedback/page")
  public ApiResponse<PageResult<Feedback>> pageFeedback(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) TicketStatus status,
    @RequestParam(defaultValue = "1") int pageNum,
    @RequestParam(defaultValue = "10") int pageSize
  ) {
    return ApiResponse.success(ticketService.pageFeedback(keyword, status, pageNum, pageSize));
  }

  @GetMapping("/feedback/{id}")
  public ApiResponse<Feedback> feedbackDetail(@PathVariable String id) {
    Feedback feedback = ticketService.getFeedbackById(id);
    return feedback == null ? ApiResponse.fail("feedback not found") : ApiResponse.success(feedback);
  }

  @GetMapping("/work-order/{id}")
  public ApiResponse<Feedback> workOrderDetail(@PathVariable String id) {
    Feedback feedback = ticketService.getFeedbackById(id);
    return feedback == null ? ApiResponse.fail("work order not found") : ApiResponse.success(feedback);
  }

  @PostMapping("/feedback")
  public ApiResponse<Feedback> createFeedback(@RequestBody @Valid CreateFeedbackRequest request) {
    return ApiResponse.success("created", ticketService.createFeedback(request));
  }

  @PostMapping("/feedback/{id}/reply")
  public ApiResponse<Feedback> replyFeedback(@PathVariable String id, @RequestBody @Valid ReplyFeedbackRequest request) {
    Feedback feedback = ticketService.replyFeedback(id, request);
    return feedback == null ? ApiResponse.fail("feedback not found") : ApiResponse.success("replied", feedback);
  }

  @GetMapping("/work-order/page")
  public ApiResponse<PageResult<WorkOrder>> pageWorkOrders(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) TicketStatus status,
    @RequestParam(defaultValue = "1") int pageNum,
    @RequestParam(defaultValue = "10") int pageSize
  ) {
    return ApiResponse.success(ticketService.pageWorkOrders(keyword, status, pageNum, pageSize));
  }

  @GetMapping("/work-order/summary")
  public ApiResponse<WorkOrderSummary> workOrderSummary(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) TicketStatus status
  ) {
    return ApiResponse.success(ticketService.getWorkOrderSummary(keyword, status));
  }

  @PostMapping("/work-order/{id}/status")
  public ApiResponse<WorkOrder> updateWorkOrderStatus(@PathVariable String id, @RequestBody @Valid UpdateWorkOrderStatusRequest request) {
    WorkOrder workOrder = ticketService.updateWorkOrderStatus(id, request);
    return workOrder == null ? ApiResponse.fail("work order not found") : ApiResponse.success("updated", workOrder);
  }

  @PostMapping("/work-order/{id}/reply")
  public ApiResponse<Feedback> replyWorkOrder(@PathVariable String id, @RequestBody @Valid ReplyFeedbackRequest request) {
    Feedback feedback = ticketService.replyFeedback(id, request);
    return feedback == null ? ApiResponse.fail("feedback not found") : ApiResponse.success("replied", feedback);
  }
}
