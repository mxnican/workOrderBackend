package com.wly.workorder.service;

import com.wly.workorder.WorkOrderApplication;
import com.wly.workorder.auth.AuthContext;
import com.wly.workorder.auth.AuthRole;
import com.wly.workorder.auth.AuthSession;
import com.wly.workorder.model.TicketModels.CreateFeedbackRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:workorder;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always"
  }
)
class DatabaseMigrationTest {
  @Autowired
  private TicketService ticketService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void creating_feedback_should_write_a_row_to_feedback_table() {
    AuthContext.set(new AuthSession("token-user", "user", "Tester", "", AuthRole.USER));
    try {
      ticketService.createFeedback(new CreateFeedbackRequest("DB title", "DB description", "Tester", List.of(), List.of()));
    } finally {
      AuthContext.clear();
    }

    Integer count = jdbcTemplate.queryForObject(
      "select count(*) from wo_feedback where title = ?",
      Integer.class,
      "DB title"
    );

    assertEquals(1, count);
  }

  @Test
  void creating_feedback_should_not_throw() {
    AuthContext.set(new AuthSession("token-user-2", "user", "Tester", "", AuthRole.USER));
    try {
      assertDoesNotThrow(() ->
        ticketService.createFeedback(new CreateFeedbackRequest("DB title 2", "DB description 2", "Tester", List.of(), List.of()))
      );
    } finally {
      AuthContext.clear();
    }
  }
}
