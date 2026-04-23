package com.wly.workorder.service;

import com.wly.workorder.common.PageResult;
import com.wly.workorder.model.TicketModels.CreateFeedbackRequest;
import com.wly.workorder.model.TicketModels.Feedback;
import com.wly.workorder.model.TicketModels.ReplyFeedbackRequest;
import com.wly.workorder.model.TicketModels.TicketStatus;
import com.wly.workorder.model.TicketModels.UpdateWorkOrderStatusRequest;
import com.wly.workorder.model.TicketModels.WorkOrder;
import com.wly.workorder.model.TicketModels.WorkOrderSummary;

public interface TicketService {
  PageResult<Feedback> pageFeedback(String keyword, TicketStatus status, int pageNum, int pageSize);

  Feedback getFeedbackById(String id);

  Feedback createFeedback(CreateFeedbackRequest request);

  Feedback replyFeedback(String id, ReplyFeedbackRequest request);

  PageResult<WorkOrder> pageWorkOrders(String keyword, TicketStatus status, int pageNum, int pageSize);

  WorkOrderSummary getWorkOrderSummary(String keyword, TicketStatus status);

  WorkOrder updateWorkOrderStatus(String id, UpdateWorkOrderStatusRequest request);
}
